package org.ulalax.playhouse.service.play.base

import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.service.play.PlayService
import java.util.*
import kotlin.collections.HashMap

typealias TimerCallback = suspend ()->Unit

class  TimerManager(private val playService: PlayService){
    private val timerIds:MutableMap<Long,TimerTask> = HashMap();
    private val timer = Timer("PlayHoseTimer",false)

    fun start(){
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
              //  SpaceOffice.loggingRoomState()
            }
        },1000,5000)
    }


    class RepeatTimerTask(
        private val playService: PlayService,
        private val stageId:Long,
        private val timerId:Long,
        private val timerCallback: TimerCallback
    ) :TimerTask(){

        override fun run()  {
            val StageTimerPacket = RoutePacket.stageTimerOf(stageId,timerId,timerCallback)
            playService.onReceive(StageTimerPacket)
        }
    }

    class CounterTimerTask(
        private val playService: PlayService,
        private val timerManager: TimerManager,
        private val stageId:Long,
        private var count:Int,
        private val timerId:Long,
        private val timerCallback: TimerCallback
    ) :TimerTask(){

        //private val log = logger()
        override fun run()  {
      //      log.info("counterTimerTask")
            if(count > 0 ){
                val stageTimerPacket = RoutePacket.stageTimerOf(stageId,timerId,timerCallback)
                playService.onReceive(stageTimerPacket)
                count--
            }else{
                timerManager.cancelTimer(timerId)
            }
        }
    }



    fun registerRepeatTimer(stageId:Long,timerId: Long, initialDelay: Long, period: Long, timerCallback: TimerCallback):Long {
        val timerTask = RepeatTimerTask(this.playService,stageId,timerId,timerCallback)
        timer.scheduleAtFixedRate(timerTask,initialDelay,period)
        timerIds[timerId] = timerTask
        return timerId
    }

    fun registerCountTimer(stageId:Long, timerId: Long, initialDelay: Long,count: Int, period: Long, timerCallback: TimerCallback):Long {
        val timerTask = CounterTimerTask(this.playService,this,stageId,count,timerId,timerCallback)
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