package org.ulalax.playhouse.base.service.room

import org.ulalax.playhouse.base.communicator.ServerInfoCenter
import org.ulalax.playhouse.base.protocol.Packet
import org.ulalax.playhouse.base.protocol.ReplyPacket
import org.ulalax.playhouse.base.service.room.base.BaseRoom
import kotlinx.coroutines.CompletableDeferred

class UserSenderImpl(val accountId:Long,
                     var sessionEndpoint:String,
                     var sid:Int,
                     var apiEndpoint:String,
                     private val baseRoom:BaseRoom,
                     private val serverInfoCenter: ServerInfoCenter,
) : UserSender {
    override fun sessionEndpoint(): String {
        return this.sessionEndpoint
    }

    override fun apiEndpoint(): String {
        return this.apiEndpoint
    }

    override fun sid(): Int {
        return this.sid
    }
    override fun accountId(): Long {
        return this.accountId
    }

    override fun leaveRoom() {
        baseRoom.leaveRoom(accountId,sessionEndpoint,sid)
    }

    override fun sendToClient(packet: Packet) {
        baseRoom.roomSender.sendToClient(sessionEndpoint,sid,packet)
    }

    override fun sendToApi(sessionInfo: String, packet: Packet) {
        var serverInfo = serverInfoCenter.findServer(apiEndpoint)
        if( !serverInfo.isValid()){
            serverInfo = serverInfoCenter.findRoundRobinServer(serverInfo.serviceId)
        }
        baseRoom.roomSender.sendToApi(serverInfo.bindEndpoint,sessionInfo,packet)
    }

    override suspend fun requestToApi(sessionInfo: String, packet: Packet): ReplyPacket {
        var serverInfo = serverInfoCenter.findServer(apiEndpoint)
        if( !serverInfo.isValid()){
            serverInfo = serverInfoCenter.findRoundRobinServer(serverInfo.serviceId)
        }
        return baseRoom.roomSender.requestToApi(serverInfo.bindEndpoint,sessionInfo,packet)
    }

    override fun deferredToApi(sessionInfo: String, packet: Packet): CompletableDeferred<ReplyPacket> {
        var serverInfo = serverInfoCenter.findServer(apiEndpoint)
        if( !serverInfo.isValid()){
            serverInfo = serverInfoCenter.findRoundRobinServer(serverInfo.serviceId)
        }
        return baseRoom.roomSender.deferredToApi(serverInfo.bindEndpoint,sessionInfo,packet)
    }

    fun update(sessionEndpoint: String, sid: Int,apiEndpoint: String) {
        this.sessionEndpoint = sessionEndpoint
        this.apiEndpoint = apiEndpoint
        this.sid = sid
    }
}