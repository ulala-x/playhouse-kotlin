package org.ulalax.playhouse.service

import org.ulalax.playhouse.protocol.Packet
import org.ulalax.playhouse.protocol.ReplyPacket
import org.ulalax.playhouse.service.api.CreateJoinStageResult
import org.ulalax.playhouse.service.api.CreateStageResult
import org.ulalax.playhouse.service.api.JoinStageResult
import org.ulalax.playhouse.service.play.base.TimerCallback
import kotlinx.coroutines.CompletableDeferred
import org.ulalax.playhouse.communicator.ServerInfo
import org.ulalax.playhouse.communicator.ServerState
import java.time.Duration


interface SystemPanel{

    fun randomServerInfo(serviceId: String) : ServerInfo
    fun serverInfo(endpoint:String) : ServerInfo
    fun serverList(): List<ServerInfo>
//    fun removeServerInfo(endpoint: String)
    fun pause()
    fun resume()
    fun shutdown()
    fun serverState(): ServerState


}
interface BaseSender {
    fun serviceId():String
    fun reply(reply: ReplyPacket)
    fun sendToClient(sessionEndpoint: String,sid:Int,packet: Packet)
    fun sendToApi(apiEndpoint:String, sessionInfo: String,packet: Packet)
    fun sendToStage(playEndpoint:String, stageId:Long, accountId:Long, packet: Packet)
//    fun callToApi(apiEndpoint:String, packet: Packet, sessionInfo: String, replyCallback: ReplyCallback)
//    fun callToRoom(playEndpoint:String, stageId:Long, accountId:Long, packet: Packet, replyCallback: ReplyCallback)
//    fun callToApi(apiEndpoint:String, sessionInfo: String,packet: Packet): ReplyPacket
//    fun callToRoom(playEndpoint:String, stageId:Long, accountId:Long, packet: Packet): ReplyPacket
    suspend fun requestToApi(apiEndpoint:String, sessionInfo: String, packet: Packet): ReplyPacket
    suspend fun requestToRoom(playEndpoint:String, stageId:Long, accountId:Long, packet: Packet): ReplyPacket

    fun asyncToApi(apiEndpoint:String, sessionInfo: String, packet: Packet): CompletableDeferred<ReplyPacket>
    fun asyncToRoom(playEndpoint:String, stageId:Long, accountId:Long, packet: Packet):CompletableDeferred<ReplyPacket>

    fun sendToSystem(endpoint: String, packet: Packet)
    suspend fun requestToSystem(endpoint: String, packet: Packet): ReplyPacket

    fun sessionClose(sessionEndpoint: String,sid:Int)

}

interface ApiBaseSender : BaseSender {

    fun updateSession(sessionEndpoint: String,sid:Int,serviceId: String,sessionInfo:String)

    fun createRoom(playEndpoint:String,StageType:String,packet: Packet): CreateStageResult
    fun joinRoom(playEndpoint:String,
                 stageId:Long,
                 accountId: Long,
                 sessionEndpoint: String,
                 sid:Int,
                 packet: Packet
    ): JoinStageResult
    fun createJoinRoom(playEndpoint:String, StageType:String, stageId:Long,
                       createPacket: Packet,
                       accountId: Long, sessionEndpoint: String, sid:Int,
                       joinPacket: Packet,
    ): CreateJoinStageResult
}
interface ApiSender : ApiBaseSender {

    fun authenticate(accountId:Long,sessionInfo:String)
    fun sessionEndpoint():String
    fun sid():Int

    fun sessionInfo():String
    fun sendToClient(packet: Packet){
        sendToClient(sessionEndpoint(),sid(),packet)
    }
    fun sessionClose(){
        sessionClose(sessionEndpoint(),sid())
    }

    fun updateSession(serviceId: String,sessionInfo:String ){
        updateSession(sessionEndpoint(),sid(),sessionInfo,serviceId)
    }


}

typealias AsyncPreCallback<T> = suspend ()->T
typealias AsyncPostCallback<T> = suspend (T)->Unit


interface StageSender : BaseSender {

    //fun reply(reply: ReplyPacket)
    fun stageId():Long
    fun stageType():String

    fun addRepeatTimer(initialDelay: Duration, period: Duration, timerCallback: TimerCallback): Long
    fun addCountTimer(initialDelay: Duration, count: Int, period: Duration, timerCallback: TimerCallback): Long
    fun cancelTimer(timerId: Long)
    fun closeStage()

    suspend fun <T> asyncBlock(preCallback: AsyncPreCallback<T>, postCallback: AsyncPostCallback<T>? = null)

}

interface ApiBackendSender : ApiBaseSender {
    fun getFromEndpoint():String
}
interface SessionSender : BaseSender {}