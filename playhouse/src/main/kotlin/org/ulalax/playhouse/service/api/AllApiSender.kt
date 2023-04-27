package org.ulalax.playhouse.service.api

import com.google.protobuf.ByteString
import org.ulalax.playhouse.communicator.ClientCommunicator
import org.ulalax.playhouse.communicator.RequestCache
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.protocol.Server
import org.ulalax.playhouse.protocol.Server.*
import org.ulalax.playhouse.service.*

class AllApiSender (override val serviceId:Short,
                    override val accountId:Long,
                    override val sessionEndpoint:String,
                    override val sid:Int,
                    private val clientCommunicator: ClientCommunicator,
                    private val reqCache: RequestCache,
) : XApiCommonSender(serviceId,clientCommunicator,reqCache),
    ApiSender, ApiBackendSender {

    override fun getFromEndpoint(): String {
        return this.currentHeader?.from ?:""
    }
//    override fun sessionEndpoint(): String {
//        return this.currentHeader?.from ?:""
//    }
//
//    override fun getSid():Int {
//        return this.currentHeader?.sid ?:0
//    }


    override fun authenticate(accountId:Long){
        val message = AuthenticateMsg.newBuilder()
            .setServiceId(serviceId.toInt())
            .setAccountId(accountId).build()

        this.currentHeader?.run {
            sendToBaseSession(from,sid, Packet(message))
        } ?: throw ApiException.NotExistApiHeaderInfoException()

    }

    override suspend fun joinStage(playEndpoint: String,
                                   stageId: Long,
                                   packet: Packet
    ): JoinStageResult {
        val req = JoinStageReq.newBuilder()
            .setSessionEndpoint(sessionEndpoint)
            .setSid(sid)
            .setPayloadId(packet.msgId)
            .setPayload(ByteString.copyFrom(packet.data())).build()

        val reply  = requestToBaseStage(playEndpoint,stageId,accountId, Packet(req))
        val res = JoinStageRes.parseFrom(reply.data())

        return JoinStageResult(reply.errorCode,res.stageIdx, Packet(res.payloadId,res.payload))

    }

    override suspend fun createJoinStage(
        playEndpoint: String,
        stageType: String,
        stageId: Long,
        createPacket: Packet,
        joinPacket: Packet,
    ): CreateJoinStageResult {
        val req = CreateJoinStageReq.newBuilder()
            .setStageType(stageType)
            .setCreatePayloadId(createPacket.msgId)
            .setCreatePayload(ByteString.copyFrom(createPacket.data()))
            .setSessionEndpoint(sessionEndpoint)
            .setSid(sid)
            .setJoinPayloadId(joinPacket.msgId)
            .setJoinPayload(ByteString.copyFrom(joinPacket.data())).build()

        val reply = requestToBaseStage(playEndpoint,stageId,accountId, Packet(req))

        val res = CreateJoinStageRes.parseFrom(reply.data())


        return CreateJoinStageResult(
            reply.errorCode,
            res.isCreated,
            res.stageIdx,
            Packet(res.createPayloadId, res.createPayload),
            Packet(res.joinPayloadId, res.joinPayload)
        )

    }

    fun clone(): AllApiSender {
        return AllApiSender(this.serviceId,this.accountId,this.sessionEndpoint,this.sid,this.clientCommunicator,this.reqCache)
    }

}