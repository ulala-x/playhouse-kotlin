package org.ulalax.playhouse.service.play

import org.ulalax.playhouse.communicator.ServerInfoCenter
import org.ulalax.playhouse.service.play.base.BaseStage
import kotlinx.coroutines.CompletableDeferred
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.communicator.message.ReplyPacket

class XActorSender(val accountId:Long,
                   var sessionEndpoint:String,
                   var sid:Int,
                   var apiEndpoint:String,
                   private val baseStage: BaseStage,
                   private val serverInfoCenter: ServerInfoCenter,
) : ActorSender {
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

    override fun leaveStage() {
        baseStage.leaveStage(accountId,sessionEndpoint,sid)
    }

    override fun sendToClient(packet: Packet) {
        baseStage.stageSenderImpl.sendToClient(sessionEndpoint,sid,packet)
    }

    override fun sendToApi(packet: Packet) {
        var serverInfo = serverInfoCenter.findServer(apiEndpoint)
        if( !serverInfo.isValid()){
            serverInfo = serverInfoCenter.findRoundRobinServer(serverInfo.serviceId)
        }
        baseStage.stageSenderImpl.sendToApi(serverInfo.bindEndpoint,accountId,packet)
    }

    override suspend fun requestToApi(packet: Packet): ReplyPacket {
        var serverInfo = serverInfoCenter.findServer(apiEndpoint)
        if( !serverInfo.isValid()){
            serverInfo = serverInfoCenter.findRoundRobinServer(serverInfo.serviceId)
        }
        return baseStage.stageSenderImpl.requestToApi(serverInfo.bindEndpoint,accountId,packet)
    }

    override fun asyncToApi( packet: Packet): CompletableDeferred<ReplyPacket> {
        var serverInfo = serverInfoCenter.findServer(apiEndpoint)
        if( !serverInfo.isValid()){
            serverInfo = serverInfoCenter.findRoundRobinServer(serverInfo.serviceId)
        }
        return baseStage.stageSenderImpl.asyncToApi(serverInfo.bindEndpoint,accountId,packet)
    }

    fun update(sessionEndpoint: String, sid: Int,apiEndpoint: String) {
        this.sessionEndpoint = sessionEndpoint
        this.apiEndpoint = apiEndpoint
        this.sid = sid
    }
}