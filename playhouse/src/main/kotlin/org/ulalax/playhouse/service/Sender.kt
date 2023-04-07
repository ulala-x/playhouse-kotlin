package org.ulalax.playhouse.service

import kotlinx.coroutines.CompletableDeferred
import org.ulalax.playhouse.communicator.ServerInfo
import org.ulalax.playhouse.communicator.ServerState
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.communicator.message.ReplyCallback
import org.ulalax.playhouse.communicator.message.ReplyPacket
import java.time.Duration


interface SystemPanel{

    fun randomServerInfo(serviceId: Short) : ServerInfo
    fun serverInfo(endpoint:String) : ServerInfo
    fun serverList(): List<ServerInfo>
    fun pause()
    fun resume()
    fun shutdown()
    fun serverState(): ServerState


}
interface CommonSender {
    fun serviceId():Short
    fun reply(reply: ReplyPacket)
    fun sendToClient(sessionEndpoint: String,sid:Int,packet: Packet)
    fun sendToApi(apiEndpoint:String, sessionInfo: String,packet: Packet)
    fun sendToStage(playEndpoint:String, stageId:Long, accountId:Long, packet: Packet)

    fun requestToApi(apiEndpoint:String, packet: Packet, sessionInfo: String, replyCallback: ReplyCallback)
    fun requestToRoom(playEndpoint:String, stageId:Long, accountId:Long, packet: Packet, replyCallback: ReplyCallback)
    suspend fun requestToApi(apiEndpoint:String, sessionInfo: String, packet: Packet): ReplyPacket
    suspend fun requestToStage(playEndpoint:String, stageId:Long, accountId:Long, packet: Packet): ReplyPacket

    fun asyncToApi(apiEndpoint:String, sessionInfo: String, packet: Packet): CompletableDeferred<ReplyPacket>
    fun asyncToStage(playEndpoint:String, stageId:Long, accountId:Long, packet: Packet):CompletableDeferred<ReplyPacket>

    fun sendToSystem(endpoint: String, packet: Packet)
    suspend fun requestToSystem(endpoint: String, packet: Packet): ReplyPacket

    fun sessionClose(sessionEndpoint: String,sid:Int)

}

interface ApiCommonSender : CommonSender {

    fun updateSession(sessionEndpoint: String,sid:Int,serviceId: Short,sessionInfo:String)

    fun createStage(playEndpoint:String, StageType:String, packet: Packet): CreateStageResult
    fun joinStage(playEndpoint:String,
                  stageId:Long,
                  accountId: Long,
                  sessionEndpoint: String,
                  sid:Int,
                  packet: Packet
    ): org.ulalax.playhouse.service.JoinStageResult
    fun createJoinStage(playEndpoint:String, StageType:String, stageId:Long,
                        createPacket: Packet,
                        accountId: Long, sessionEndpoint: String, sid:Int,
                        joinPacket: Packet,
    ): org.ulalax.playhouse.service.CreateJoinStageResult
}
interface ApiSender : ApiCommonSender {

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

    fun updateSession(serviceId: Short,sessionInfo:String ){
        updateSession(sessionEndpoint(),sid(),serviceId,sessionInfo)
    }


}

typealias AsyncPreCallback<T> = suspend ()->T
typealias AsyncPostCallback<T> = suspend (T)->Unit


interface StageSender : CommonSender {

    //fun reply(reply: ReplyPacket)
    fun stageId():Long
    fun stageType():String

    fun addRepeatTimer(initialDelay: Duration, period: Duration, timerCallback: TimerCallback): Long
    fun addCountTimer(initialDelay: Duration, count: Int, period: Duration, timerCallback: TimerCallback): Long
    fun cancelTimer(timerId: Long)
    fun closeStage()

    suspend fun <T> asyncBlock(preCallback: AsyncPreCallback<T>, postCallback: AsyncPostCallback<T>? = null)

}

interface ApiBackendSender : ApiCommonSender {
    fun getFromEndpoint():String
}
interface SessionSender : CommonSender {}