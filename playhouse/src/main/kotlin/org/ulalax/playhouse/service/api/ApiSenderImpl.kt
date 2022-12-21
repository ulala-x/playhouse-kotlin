package org.ulalax.playhouse.service.api

import com.google.protobuf.ByteString
import org.ulalax.playhouse.communicator.CommunicateClient
import org.ulalax.playhouse.protocol.Packet
import org.apache.logging.log4j.kotlin.logger
import org.ulalax.playhouse.protocol.Server.*
import org.ulalax.playhouse.service.*

open class ApiBaseSenderImpl(serviceId:String,
                             communicateClient: CommunicateClient,
                             reqCache: RequestCache
): BaseSenderImpl(serviceId,communicateClient,reqCache), ApiBaseSender {
    override fun updateSession(sessionEndpoint: String,sid:Int,serviceId: String,sessionInfo:String){
        val message = UpdateSessionInfoMsg.newBuilder()
            .setServiceId(serviceId)
            .setSessionInfo(sessionInfo).build()
        sendToBaseSession(sessionEndpoint,sid, Packet(message))
    }

    override fun createRoom(playEndpoint:String,StageType:String,packet: Packet): CreateStageResult {
        val req = CreateStageReq.newBuilder()
            .setStageType(StageType)
            .setPayloadName(packet.msgName)
            .setPayload(ByteString.copyFrom(packet.buffer())).build()

        val reply = callToBaseRoom(playEndpoint,0,0, Packet(req))
        val res = CreateStageRes.parseFrom(reply.buffer())
        return CreateStageResult(reply.errorCode,res.stageId, Packet(res.payloadName,res.payload))
    }

    override fun joinRoom(playEndpoint: String, stageId: Long,
                          accountId: Long,sessionEndpoint: String,sid:Int,
                          packet: Packet
    ): JoinStageResult {
        val req = JoinStageReq.newBuilder()
            .setSessionEndpoint(sessionEndpoint)
            .setSid(sid)
            .setPayloadName(packet.msgName)
            .setPayload(ByteString.copyFrom(packet.buffer())).build()

        val reply  = callToBaseRoom(playEndpoint,stageId,accountId, Packet(req))
        val res = JoinStageRes.parseFrom(reply.buffer())

        return JoinStageResult(reply.errorCode, Packet(res.payloadName,res.payload))

    }

    override fun createJoinRoom(
        playEndpoint: String, StageType: String, stageId: Long,
        createPacket: Packet,
        accountId: Long, sessionEndpoint: String, sid:Int,
        joinPacket: Packet,
    ): CreateJoinStageResult {
        val req = CreateJoinStageReq.newBuilder()
            .setStageType(StageType)
            .setCreatePayloadName(createPacket.msgName)
            .setCreatePayload(ByteString.copyFrom(createPacket.buffer()))
            .setSessionEndpoint(sessionEndpoint)
            .setSid(sid)
            .setJoinPayloadName(joinPacket.msgName)
            .setJoinPayload(ByteString.copyFrom(joinPacket.buffer())).build()

        val reply = callToBaseRoom(playEndpoint,stageId,accountId, Packet(req))
        val res = CreateJoinStageRes.parseFrom(reply.buffer())
        return CreateJoinStageResult(
            reply.errorCode,
            res.isCreated,
            Packet(res.createPayloadName,res.createPayload),
            Packet(res.joinPayloadName,res.joinPayload)
        )

    }
}

class ApiSenderImpl (private val serviceId:String,
                     private val communicateClient: CommunicateClient,
                     private val reqCache: RequestCache
) : ApiBaseSenderImpl(serviceId,communicateClient,reqCache),
    ApiSender, ApiBackendSender {
    private val log = logger()

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
            .setServiceId(serviceId)
            .setAccountId(accountId)
            .setSessionInfo(sessionInfo).build()

        this.currentHeader?.run {
            sendToBaseSession(from,sid, Packet(message))
        } ?: throw ApiException.NotExistApiHeaderInfoException()

    }


    fun clone(): ApiSenderImpl {
        return ApiSenderImpl(this.serviceId,this.communicateClient,this.reqCache)
    }

}