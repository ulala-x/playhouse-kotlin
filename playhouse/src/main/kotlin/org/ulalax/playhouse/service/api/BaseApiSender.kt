package org.ulalax.playhouse.service.api

import com.google.protobuf.ByteString
import org.ulalax.playhouse.communicator.ClientCommunicator
import org.ulalax.playhouse.communicator.RequestCache
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.protocol.Server.*
import org.ulalax.playhouse.service.*

open class ApiBaseSender(serviceId:Short,
                         clientCommunicator: ClientCommunicator,
                         reqCache: RequestCache
): BaseSender(serviceId,clientCommunicator,reqCache), ApiCommonSender {
    override fun updateSession(sessionEndpoint: String,sid:Int,serviceId: Short,sessionInfo:String){
        val message = UpdateSessionInfoMsg.newBuilder()
            .setServiceId(serviceId.toInt())
            .setSessionInfo(sessionInfo).build()
        sendToBaseSession(sessionEndpoint,sid, Packet(message))
    }

    override fun createStage(playEndpoint:String, stageType:String, packet: Packet): CreateStageResult {
        val req = CreateStageReq.newBuilder()
            .setStageType(stageType)
            .setPayloadId(packet.msgId)
            .setPayload(ByteString.copyFrom(packet.data())).build()

        val reply = callToBaseRoom(playEndpoint,0,0, Packet(req))
        val res = CreateStageRes.parseFrom(reply.data())
        return CreateStageResult(reply.errorCode,res.stageId, Packet(res.payloadId,res.payload))
    }

    override fun joinStage(playEndpoint: String, stageId: Long,
                           accountId: Long, sessionEndpoint: String, sid:Int,
                           packet: Packet
    ): org.ulalax.playhouse.service.JoinStageResult {
        val req = JoinStageReq.newBuilder()
            .setSessionEndpoint(sessionEndpoint)
            .setSid(sid)
            .setPayloadId(packet.msgId)
            .setPayload(ByteString.copyFrom(packet.data())).build()

        val reply  = callToBaseRoom(playEndpoint,stageId,accountId, Packet(req))
        val res = JoinStageRes.parseFrom(reply.data())

        return org.ulalax.playhouse.service.JoinStageResult(reply.errorCode, Packet(res.payloadId, res.payload))

    }

    override fun createJoinStage(
            playEndpoint: String, stageType: String, stageId: Long,
            createPacket: Packet,
            accountId: Long, sessionEndpoint: String, sid:Int,
            joinPacket: Packet,
    ): org.ulalax.playhouse.service.CreateJoinStageResult {
        val req = CreateJoinStageReq.newBuilder()
            .setStageType(stageType)
            .setCreatePayloadId(createPacket.msgId)
            .setCreatePayload(ByteString.copyFrom(createPacket.data()))
            .setSessionEndpoint(sessionEndpoint)
            .setSid(sid)
            .setJoinPayloadId(joinPacket.msgId)
            .setJoinPayload(ByteString.copyFrom(joinPacket.data())).build()

        val reply = callToBaseRoom(playEndpoint,stageId,accountId, Packet(req))
        val res = CreateJoinStageRes.parseFrom(reply.data())
        return org.ulalax.playhouse.service.CreateJoinStageResult(
                reply.errorCode,
                res.isCreated,
                Packet(res.createPayloadId, res.createPayload),
                Packet(res.joinPayloadId, res.joinPayload)
        )

    }
}

class BaseApiSender (private val serviceId:Short,
                     private val clientCommunicator: ClientCommunicator,
                     private val reqCache: RequestCache
) : ApiBaseSender(serviceId,clientCommunicator,reqCache),
    ApiSender, ApiBackendSender {

    override fun getFromEndpoint(): String {
        return this.currentHeader?.from ?:""
    }
    override fun sessionEndpoint(): String {
        return this.currentHeader?.from ?:""
    }

    override fun sid():Int {
        return this.currentHeader?.sid ?:0
    }

    override fun sessionInfo(): String {
        return this.currentHeader?.sessionInfo ?: ""
    }


    override fun authenticate(accountId:Long,sessionInfo:String){
        val message = AuthenticateMsg.newBuilder()
            .setServiceId(serviceId.toInt())
            .setAccountId(accountId)
            .setSessionInfo(sessionInfo).build()

        this.currentHeader?.run {
            sendToBaseSession(from,sid, Packet(message))
        } ?: throw ApiException.NotExistApiHeaderInfoException()

    }


    fun clone(): BaseApiSender {
        return BaseApiSender(this.serviceId,this.clientCommunicator,this.reqCache)
    }

}