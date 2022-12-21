package org.ulalax.playhouse.base.service

import org.ulalax.playhouse.base.communicator.ServerInfo
import org.ulalax.playhouse.base.protocol.Packet
import org.ulalax.playhouse.base.protocol.ReplyCallback
import org.ulalax.playhouse.base.protocol.ReplyPacket
import org.ulalax.playhouse.base.service.api.CreateJoinRoomResult
import org.ulalax.playhouse.base.service.api.CreateRoomResult
import org.ulalax.playhouse.base.service.api.JoinRoomResult
import org.ulalax.playhouse.base.service.room.base.TimerCallback
import kotlinx.coroutines.CompletableDeferred
import java.time.Duration


interface SystemPanel{

    fun randomServerInfo(serviceId: String) : ServerInfo
    fun serverInfo(endpoint:String) : ServerInfo
    fun serverList(): List<ServerInfo>
//    fun removeServerInfo(endpoint: String)
    fun pause()
    fun resume()
    fun shutdown()
    fun serverState():ServerInfo.ServerState


}
interface BaseSender {
    fun serviceId():String
    fun reply(reply: ReplyPacket)
    fun sendToClient(sessionEndpoint: String,sid:Int,packet: Packet)
    fun sendToApi(apiEndpoint:String, sessionInfo: String,packet: Packet)
    fun sendToRoom(roomEndpoint:String, roomId:Long, accountId:Long, packet: Packet)
    fun callToApi(apiEndpoint:String, packet: Packet, sessionInfo: String, replyCallback: ReplyCallback)
    fun callToRoom(roomEndpoint:String, roomId:Long, accountId:Long, packet: Packet, replyCallback: ReplyCallback)
    fun callToApi(apiEndpoint:String, sessionInfo: String,packet: Packet ):ReplyPacket
    fun callToRoom(roomEndpoint:String, roomId:Long, accountId:Long, packet: Packet):ReplyPacket
    suspend fun requestToApi(apiEndpoint:String, sessionInfo: String, packet: Packet): ReplyPacket
    suspend fun requestToRoom(roomEndpoint:String, roomId:Long, accountId:Long, packet: Packet):ReplyPacket

    fun deferredToApi(apiEndpoint:String, sessionInfo: String, packet: Packet): CompletableDeferred<ReplyPacket>
    fun deferredToRoom(roomEndpoint:String, roomId:Long, accountId:Long, packet: Packet):CompletableDeferred<ReplyPacket>

    fun sendToSystem(endpoint: String, packet: Packet)
    fun callToSystem(endpoint: String, packet: Packet): ReplyPacket

    fun sessionClose(sessionEndpoint: String,sid:Int)

}

interface ApiBaseSender : BaseSender {

    fun updateSession(sessionEndpoint: String,sid:Int,serviceId: String,sessionInfo:String)

    fun createRoom(roomEndpoint:String,roomType:String,packet: Packet): CreateRoomResult
    fun joinRoom(roomEndpoint:String,
                 roomId:Long,
                 accountId: Long,
                 sessionEndpoint: String,
                 sid:Int,
                 packet: Packet):JoinRoomResult
    fun createJoinRoom(roomEndpoint:String,roomType:String,roomId:Long,
                       createPacket: Packet,
                       accountId: Long,sessionEndpoint: String,sid:Int,
                       joinPacket: Packet,
    ):CreateJoinRoomResult
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


interface RoomSender : BaseSender {

    //fun reply(reply: ReplyPacket)
    fun roomId():Long
    fun roomType():String

    fun addRepeatTimer(initialDelay: Duration, period: Duration, timerCallback: TimerCallback): Long
    fun addCountTimer(initialDelay: Duration, count: Int, period: Duration, timerCallback: TimerCallback): Long
    fun cancelTimer(timerId: Long)
    fun closeRoom()

    suspend fun <T> asyncBlock(preCallback: AsyncPreCallback<T>, postCallback: AsyncPostCallback<T>? = null)



}

interface ApiBackendSender : ApiBaseSender{
    fun getFromEndpoint():String
}
interface SessionSender : BaseSender {}