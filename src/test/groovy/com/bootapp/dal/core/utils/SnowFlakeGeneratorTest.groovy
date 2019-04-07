package com.bootapp.dal.core.utils

import com.bootapp.dal.core.repository.IDGenInstanceRepository
import com.bootapp.dal.core.utils.idgen.IDGenerator
import com.bootapp.dal.core.utils.idgen.impl.CASSnowFlakeGenerator
import com.bootapp.dal.core.utils.idgen.impl.SnowFlakeGenerator
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import spock.lang.Specification

import javax.annotation.Resource

@DataJpaTest
class SnowFlakeGeneratorTest extends Specification implements Thread.UncaughtExceptionHandler {
    @Resource
    private IDGenInstanceRepository repository

    def "creating machines"() {
        when: "creating 32 snowflake instances..."
        def array = new ArrayList<SnowFlakeGenerator>()
        for (int i = 0; i < 32; i++) {
            array.add(new SnowFlakeGenerator(repository))
        }
        then: "no error"
        noExceptionThrown()
        when: "create 1 more instance..."
        array.add(new SnowFlakeGenerator(repository))
        then: "throw error"
        def err = thrown(Exception)
        err.message == 'too many machines in a single data center!'
    }

    def hasException = false
    def "ensure no conflict"() {
        when:
        def totalN = 40960
        IDGenerator[] machines = new IDGenerator[machineN]
        for (int i = 0; i < machineN; i++) machines[i] = new CASSnowFlakeGenerator(repository)
        def loopN = (totalN / threadN / machineN).toInteger()   // 10000
        Integer count = new Integer(0)
        Long sumTime = new Long(0)
        Thread[] threads = new Thread[threadN * machineN]
        Map<Long, Long> allData = new HashMap<>()
        for (int k = 0; k < machineN; k++) {
            def curGen = machines[k]
            for (int j = 0; j < threadN; j++) {
                threads[k * threadN + j] = new Thread() {
                    @Override
                    void run() {
                        long[] data = new long[loopN]
                        long totalTime = 0
                        for (int i = 0; i < loopN; i++) {
                            def startTime = System.nanoTime()
                            def id = curGen.nextId()
                            def endTime = System.nanoTime()
                            totalTime += endTime - startTime
                            if (id == 0L) {
                                synchronized (count) {
                                    count++
                                }
                            }
                            data[i] = id
                        }
                        synchronized (allData) {
                            for (int i = 0; i < loopN; i++) {
                                if (allData.containsKey(data[i])) {
                                    throw new RuntimeException("id conflict")
                                } else {
                                    allData.put(data[i], data[i])
                                }
                            }
                        }
                        synchronized (sumTime) {
                            sumTime += totalTime
                        }
                    }
                }
                threads[k * threadN + j].uncaughtExceptionHandler = this
            }
        }
        for (int k = 0; k < machineN; k++) {
            for (int j = 0; j < threadN; j++) {
                threads[k * threadN + j].start()
            }
        }
        for (int k = 0; k < machineN; k++) {
            for (int j = 0; j < threadN; j++) {
                threads[k * threadN + j].join()
            }
        }
        then:
        !hasException
        where:
        machineN | threadN
        1        | 100
        10       | 10
    }

    def "test QPS"() {
        def generator = new CASSnowFlakeGenerator(repository)
        def totalN = 40960*5
        def threadN = 8 // < 1000
        def loopN = totalN/threadN
        Integer count = new Integer(0)
        Long sumTime = new Long(0)
        Thread[] threads = new Thread[threadN]
        for (int j = 0; j < threadN; j++) {
            threads[j] = new Thread() {
                @Override
                void run() {
                    long totalTime = 0
                    for (int i = 0; i < loopN; i++) {
                        def startTime = System.nanoTime()
                        generator.nextId()
                        def endTime = System.nanoTime()
                        totalTime += endTime - startTime
                    }
                    synchronized (sumTime) {
                        sumTime += totalTime
                    }
                }
            }
        }
        long lastMilis = System.currentTimeMillis()
        for (int j = 0; j < threadN; j++) threads[j].start()
        for (int j = 0; j < threadN; j++) threads[j].join()
        long time = System.currentTimeMillis() - lastMilis

        println("QPS: " + (1e3 * threadN * loopN / time))
        println("avg time of generation: " + sumTime / (threadN * loopN * 1e6) + " ms")
        println("0 count:" + count)
        when:
        generator.nextId()
        then:
        noExceptionThrown()
    }

    @Override
    void uncaughtException(Thread t, Throwable e) {
        hasException = true
        println(e.stackTrace)
        println(e)
    }
}