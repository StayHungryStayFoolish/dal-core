package com.bootapp.dal.core.utils.idgen.impl

import com.bootapp.dal.core.domain.IDGenInstance
import com.bootapp.dal.core.repository.IDGenInstanceRepository
import com.bootapp.dal.core.utils.idgen.IDGenerator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import javax.transaction.Transactional

@Component
@Transactional
class SnowFlakeGenerator(@Autowired private val genRepository: IDGenInstanceRepository) : IDGenerator() {
    companion object {
        private const val START_STAMP : Long = 1554355855000L
        // sig[1] | timestamp[41] | data center[5] | machine [5] | sequence [12]
        private const val SEQUENCE_BITS : Int = 12 // sequence
        private const val MACHINE_BITS : Int = 5
        private const val DATA_CENTER_BITS : Int = 5
        //it is required that the above numbers < 31;
        private const val MAX_DATA_CENTER_NUM : Int = -1 xor (-1 shl DATA_CENTER_BITS)
        private const val MAX_MACHINE_NUM : Int = -1 xor (-1 shl MACHINE_BITS)
        private const val MAX_SEQUENCE_NUM : Long = (-1 xor (-1 shl SEQUENCE_BITS)).toLong()
        private const val MAX_SEQUENCE_NUM_MINUS_1 : Long = MAX_SEQUENCE_NUM - 1
        private const val MACHINE_SHL : Int = SEQUENCE_BITS
        private const val DATA_CENTER_SHL : Int = MACHINE_SHL + MACHINE_BITS
        private const val TIMESTAMP_SHL : Int = DATA_CENTER_SHL + DATA_CENTER_BITS
        private const val GEN_ID_RENEW_WINDOW : Long = 12 * 60 * 60 * 1000
    }
    private var dataCenterId : Long = 0
    @Volatile
    private var sequence : Long = 0
    @Volatile
    private var lastTimestamp : Long = -1L
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
            synchronized(this) {
                var curTimestamp = System.currentTimeMillis() - START_STAMP
                var ok = true
                if (curTimestamp < lastTimestamp) {
                    ok = false
                } else if (curTimestamp > lastTimestamp) {
                    sequence = 0L
                } else if (curTimestamp == lastTimestamp) {
                    sequence = (sequence + 1) % MAX_SEQUENCE_NUM
                    if (sequence == 0L) {
                        curTimestamp ++
                    }
                }
                if (ok) {
                    lastTimestamp = curTimestamp
                    return (curTimestamp shl TIMESTAMP_SHL) or
                            (dataCenterId shl DATA_CENTER_SHL) or
                            (genInstance!!.id shl MACHINE_SHL) or
                            sequence
                }
            }
        } while (true)
    }
}