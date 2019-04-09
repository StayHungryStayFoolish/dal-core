package com.bootapp.dal.core.utils.idgen.impl

import com.bootapp.dal.core.domain.IDGenInstance
import com.bootapp.dal.core.repository.IDGenInstanceRepository
import com.bootapp.dal.core.utils.idgen.IDGenerator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLongArray
import javax.transaction.Transactional

/*
Thread num,   nonCAS v.s CAS
1    thread: QPS: 4001563 v.s 3974383;  avgTime(ms):  0.00019288 v.s. 0.000190220
4    thread: QPS: 3972589 v.s 4032258;  avgTime(ms):  0.00089240 v.s. 0.000845000
8    thread: QPS: 3810431 v.s 4027588;  avgTime(ms):  0.00195459 v.s. 0.001655330
100  thread: QPS: 3118623 v.s 3479442;  avgTime(ms):  0.02729554 v.s. 0.009070220
1000 thread: QPS: 3249761 v.s 2363258;  avgTime(ms):  0.20897113 v.s. 0.010685175
*/

@Component
@Transactional
class CASSnowFlakeGenerator(@Autowired private val genRepository: IDGenInstanceRepository) : IDGenerator() {
    companion object {
        private const val START_STAMP : Long = 1554355855000L
        // sig[1] | timestamp[41] | data center[5] | machine [5] | sequence [12]
        private const val MACHINE_SHL : Int = 12
        private const val DATA_CENTER_SHL : Int = 17
        private const val TIMESTAMP_SHL : Int = 22
        //it is required that the above numbers < 31;
        private const val MAX_DATA_CENTER_NUM : Int =  31//-1 xor (-1 shl DATA_CENTER_BITS)
        private const val MAX_MACHINE_NUM : Int = 31 //-1 xor (-1 shl MACHINE_BITS)
        private const val MAX_SEQUENCE_NUM : Long = 4095L //(-1 xor (-1 shl SEQUENCE_BITS)).toLong()
        private const val GEN_ID_RENEW_WINDOW : Long = 12 * 60 * 60 * 1000
        private const val SEQUENCE_RING_CAPACITY : Int = 200
    }
    private var dataCenterId : Long = 0
    private var sequences : AtomicLongArray = AtomicLongArray(SEQUENCE_RING_CAPACITY)
    private var genInstance : IDGenInstance? = null

    init {
        genInstance = IDGenInstance(genRepository.count(), 0)
        if (genInstance!!.id > MAX_MACHINE_NUM) {
            throw Exception("too many machines in a single data center!")
        }
        this.renewGenId()
    }

    @Scheduled(fixedRate = GEN_ID_RENEW_WINDOW)
    fun renewGenId () {
        genInstance!!.updateAt = System.currentTimeMillis()
        genRepository.save(genInstance!!)
    }

    override fun nextId(): Long {
        do {
            val curTimeStamp = System.currentTimeMillis() - START_STAMP
            val index = (curTimeStamp % SEQUENCE_RING_CAPACITY).toInt()
            val lastId = sequences.get(index)
            val lastTimestamp = lastId shr TIMESTAMP_SHL
            if (lastTimestamp == 0L || lastTimestamp < curTimeStamp) {
                val newId = (curTimeStamp shl TIMESTAMP_SHL) or
                        (dataCenterId shl DATA_CENTER_SHL) or
                        (genInstance!!.id shl MACHINE_SHL)
                if (sequences.compareAndSet(index, lastId, newId)) {
                    return newId
                }
            } else if (lastTimestamp == curTimeStamp) {
                val sequence = lastId and MAX_SEQUENCE_NUM
                if (sequence < MAX_SEQUENCE_NUM) {
                    val newId = lastId + 1L
                    if (sequences.compareAndSet(index, lastId, newId)) {
                        return newId
                    }
                }
            }
        } while (true)
    }
}