package org.ulalax.playhouse.communicator

import org.apache.commons.lang3.time.StopWatch
import org.ulalax.playhouse.Logger
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class PerformanceTester(private val showQps: Boolean, private val log: Logger, private val from: String = "Server") {

    private val stopWatch = StopWatch()
    private val counter = AtomicInteger()
    private val timer = Timer("PerformanceTimer",true)

    fun incCounter(){
        if(showQps){
            counter.incrementAndGet()
        }
    }

    fun stop(){
        if(showQps){
            timer.cancel()
        }
    }

    fun start(){
        if (showQps){
            stopWatch.start()
            timer.scheduleAtFixedRate(object:TimerTask(){
                override fun run() {
                    qps()
                }
            },1000,1000)
        }
    }

    private fun qps(){
        try{
            stopWatch.stop()
            val messageCount = counter.get()
            val seconds = stopWatch.getTime(TimeUnit.SECONDS)

            val qps =  if(messageCount == 0 || seconds == 0L)  0 else messageCount / seconds

//            log.info("$from, $messageCount, 수행시간: ${stopWatch.getTime(TimeUnit.MILLISECONDS)} ms", )
            log.info("$from, $messageCount, qps: $qps",this::class.simpleName)
//            log.info("$from, The time is now ${LocalDateTime.now()}")
        }finally {
            stopWatch.reset()
            stopWatch.start()
            counter.set(0)
        }
    }

}