package org.ulalax.playhouse.base.service.room

import org.ulalax.playhouse.base.communicator.CommunicateClient
import org.ulalax.playhouse.base.communicator.message.AsyncBlockPacket
import org.ulalax.playhouse.base.communicator.message.RoutePacket
import org.ulalax.playhouse.base.protocol.Packet
import org.ulalax.playhouse.base.service.*
import org.ulalax.playhouse.base.service.room.base.TimerCallback
import org.ulalax.playhouse.base.service.room.base.TimerIdMaker
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.ulalax.playhouse.base.Server
import org.ulalax.playhouse.base.Server.*
import java.time.Duration

class RoomSenderImpl(
    private val serviceId:String,
    private val roomId:Long,
    private val roomService: RoomService,
    private val communicateClient: CommunicateClient,
    reqCache:RequestCache,
) : BaseSenderImpl(serviceId, communicateClient,reqCache),RoomSender {

    private val timerIds = HashSet<Long>()

    var roomType:String = ""

    override fun roomId(): Long {
        return roomId
    }

    override fun roomType(): String {
        return roomType
    }
    private fun makeTimerId(): Long {
        return TimerIdMaker.makeId()
    }

    override fun addRepeatTimer(initialDelay: Duration,
                                period: Duration,
                                timerCallback: TimerCallback): Long {
        val timerId = makeTimerId()
        val packet = RoutePacket.addTimerOf(
            TimerMsg.Type.REPEAT,
            this.roomId,
            timerId,
            timerCallback,
            initialDelay,
            period
        )
        roomService.onReceive(packet)
        this.timerIds.add(timerId)
        return timerId
    }

    override fun addCountTimer(
        initialDelay: Duration,
        count: Int,
        period: Duration,
        timerCallback: TimerCallback,
    ): Long {
        val timerId = makeTimerId()
        val packet = RoutePacket.addTimerOf(TimerMsg.Type.COUNT,this.roomId,timerId,timerCallback,initialDelay,period,count)
        roomService.onReceive(packet)
        this.timerIds.add(timerId)
        return timerId
    }

    override fun cancelTimer(timerId: Long) {
        val packet = RoutePacket.addTimerOf(TimerMsg.Type.CANCEL,this.roomId,timerId,{}, Duration.ZERO, Duration.ZERO)
        roomService.onReceive(packet)
        this.timerIds.remove(timerId)
    }

    override fun closeRoom() {

        timerIds.forEach{timerId->
            val packet = RoutePacket.addTimerOf(TimerMsg.Type.CANCEL,this.roomId,timerId,{}, Duration.ZERO, Duration.ZERO)
            roomService.onReceive(packet)
        }
        timerIds.clear()

        val packet = RoutePacket.roomOf(roomId,0,
            Packet(DestroyRoom.getDescriptor().name),isBase = true,isBackend = false)
        roomService.onReceive(packet)
    }

    override suspend fun <T> asyncBlock(preCallback: AsyncPreCallback<T>, postCallback: AsyncPostCallback<T>? ): Unit = coroutineScope {
        launch(ThreadPoolController.coroutineAsyncCallContext) {
            val result = preCallback()
            if(postCallback!=null){
                roomService.onReceive(AsyncBlockPacket.of(roomId,postCallback,result))
            }

        }
    }


    fun hasTimer(timerId: Long): Boolean {
        return timerIds.contains(timerId)
    }

}