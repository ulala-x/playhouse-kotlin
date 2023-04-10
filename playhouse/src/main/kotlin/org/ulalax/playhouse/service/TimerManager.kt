package org.ulalax.playhouse.service

import org.ulalax.playhouse.communicator.Processor
import org.ulalax.playhouse.communicator.message.RoutePacket
import java.util.*
import kotlin.collections.HashMap

class  TimerManager(private val processor: Processor){
    private val timerIds:MutableMap<Long, TimerTask> = HashMap();
    private val timer = Timer("PlayHoseTimer", false)

    fun start(){
//        timer.scheduleAtFixedRate(object : TimerTask() {
//            override fun run() {
//              //  SpaceOffice.loggingRoomState()
//            }
//        },1000,5000)
    }


    class RepeatTimerTask(
        private val processor: Processor,
        private val stageId:Long,
        private val timerId:Long,
        private val timerCallback: TimerCallback
    ) : TimerTask(){

        override fun run()  {
            val StageTimerPacket = RoutePacket.stageTimerOf(stageId, timerId, timerCallback)
            processor.onReceive(StageTimerPacket)
        }
    }

    class CounterTimerTask(
        private val processor: Processor,
        private val timerManager: TimerManager,
        private val stageId:Long,
        private var count:Int,
        private val timerId:Long,
        private val timerCallback: TimerCallback
    ) : TimerTask(){

        //private val log = logger()
        override fun run()  {
      //      log.info("counterTimerTask")
            if(count > 0 ){
                val stageTimerPacket = RoutePacket.stageTimerOf(stageId, timerId, timerCallback)
                processor.onReceive(stageTimerPacket)
                count--
            }else{
                timerManager.cancelTimer(timerId)
            }
        }
    }



    fun registerRepeatTimer(stageId:Long,timerId: Long, initialDelay: Long, period: Long, timerCallback: TimerCallback):Long {
        val timerTask = RepeatTimerTask(this.processor,stageId,timerId,timerCallback)
        timer.scheduleAtFixedRate(timerTask,initialDelay,period)
        timerIds[timerId] = timerTask
        return timerId
    }

    fun registerCountTimer(stageId:Long, timerId: Long, initialDelay: Long,count: Int, period: Long, timerCallback: TimerCallback):Long {
        val timerTask = CounterTimerTask(this.processor,this,stageId,count,timerId,timerCallback)
        timer.scheduleAtFixedRate(timerTask,initialDelay,period)
        timerIds[timerId] = timerTask
        return timerId
    }

    fun cancelTimer(timerId: Long) {
        timerIds[timerId]?.run{
            this.cancel()
            timer.purge()

        }
    }

}