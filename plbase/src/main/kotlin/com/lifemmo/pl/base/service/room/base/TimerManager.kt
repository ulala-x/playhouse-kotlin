package com.lifemmo.pl.base.service.room.base

import com.lifemmo.pl.base.communicator.message.RoutePacket
import com.lifemmo.pl.base.service.room.RoomService
import org.apache.logging.log4j.kotlin.logger
import java.util.*
import kotlin.collections.HashMap

typealias TimerCallback = suspend ()->Unit

class  TimerManager(private val roomService:RoomService){
    private val timerIds:MutableMap<Long,TimerTask> = HashMap();
    private val timer = Timer("timer",false)


    private val log = logger()


    fun start(){
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
              //  SpaceOffice.loggingRoomState()
            }
        },1000,5000)
    }


    class RepeatTimerTask(
            private val roomService: RoomService,
            private val roomId:Long,
            private val timerId:Long,
            private val timerCallback: TimerCallback) :TimerTask(){

        override fun run()  {
            val roomTimerPacket = RoutePacket.roomTimerOf(roomId,timerId,timerCallback)
            roomService.onReceive(roomTimerPacket)
        }
    }

    class CounterTimerTask(
        private val roomService: RoomService,
        private val timerManager:TimerManager,
        private val roomId:Long,
        private var count:Int,
        private val timerId:Long,
        private val timerCallback:TimerCallback) :TimerTask(){

        //private val log = logger()
        override fun run()  {
      //      log.info("counterTimerTask")
            if(count > 0 ){
                val roomTimerPacket = RoutePacket.roomTimerOf(roomId,timerId,timerCallback)
                roomService.onReceive(roomTimerPacket)
                count--
            }else{
                timerManager.cancelTimer(timerId)
            }
        }
    }



    fun registerRepeatTimer(roomId:Long,timerId: Long, initialDelay: Long, period: Long, timerCallback: TimerCallback):Long {
        val timerTask = RepeatTimerTask(this.roomService,roomId,timerId,timerCallback)
        timer.scheduleAtFixedRate(timerTask,initialDelay,period)
        timerIds[timerId] = timerTask
        return timerId
    }

    fun registerCountTimer(roomId:Long, timerId: Long, initialDelay: Long,count: Int, period: Long, timerCallback:TimerCallback):Long {
        val timerTask = CounterTimerTask(this.roomService,this,roomId,count,timerId,timerCallback)
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