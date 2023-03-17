package org.ulalax.playhouse.service.play

import org.ulalax.playhouse.communicator.ClientCommunicator
import org.ulalax.playhouse.communicator.message.AsyncBlockPacket
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.service.TimerCallback
import org.ulalax.playhouse.service.TimerIdMaker
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.ulalax.playhouse.communicator.RequestCache
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.protocol.Server.*
import org.ulalax.playhouse.service.*
import java.time.Duration

class BaseStageSender(
        private val serviceId:String,
        private val stageId:Long,
        private val playService: PlayService,
        private val clientCommunicator: ClientCommunicator,
        reqCache: RequestCache,
) : BaseSender(serviceId, clientCommunicator,reqCache), StageSender {

    private val timerIds = HashSet<Long>()
    var stageType:String = ""

    override fun stageId(): Long {
        return stageId
    }

    override fun stageType(): String {
        return stageType
    }
    private fun makeTimerId(): Long {
        return TimerIdMaker.makeId()
    }

    override fun addRepeatTimer(initialDelay: Duration,
                                period: Duration,
                                timerCallback: TimerCallback
    ): Long {
        val timerId = makeTimerId()
        val packet = RoutePacket.addTimerOf(
            TimerMsg.Type.REPEAT,
            this.stageId,
            timerId,
            timerCallback,
            initialDelay,
            period
        )
        playService.onReceive(packet)
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
        val packet = RoutePacket.addTimerOf(TimerMsg.Type.COUNT,this.stageId,timerId,timerCallback,initialDelay,period,count)
        playService.onReceive(packet)
        this.timerIds.add(timerId)
        return timerId
    }

    override fun cancelTimer(timerId: Long) {
        val packet = RoutePacket.addTimerOf(TimerMsg.Type.CANCEL,this.stageId,timerId,{}, Duration.ZERO, Duration.ZERO)
        playService.onReceive(packet)
        this.timerIds.remove(timerId)
    }

    override fun closeStage() {

        timerIds.forEach{timerId->
            val packet = RoutePacket.addTimerOf(TimerMsg.Type.CANCEL,this.stageId,timerId,{}, Duration.ZERO, Duration.ZERO)
            playService.onReceive(packet)
        }
        timerIds.clear()

        val packet = RoutePacket.stageOf(stageId,0,
            Packet(DestroyStage.getDescriptor().name),isBase = true,isBackend = false)
        playService.onReceive(packet)
    }

    override suspend fun <T> asyncBlock(preCallback: AsyncPreCallback<T>, postCallback: AsyncPostCallback<T>? ): Unit = coroutineScope {
        launch(ThreadPoolController.coroutineAsyncCallContext) {
            val result = preCallback()
            if(postCallback!=null){
                playService.onReceive(AsyncBlockPacket.of(stageId,postCallback,result))
            }

        }
    }


    fun hasTimer(timerId: Long): Boolean {
        return timerIds.contains(timerId)
    }

}