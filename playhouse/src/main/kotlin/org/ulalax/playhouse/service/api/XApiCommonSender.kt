package org.ulalax.playhouse.service.api

import com.google.protobuf.ByteString
import org.ulalax.playhouse.communicator.ClientCommunicator
import org.ulalax.playhouse.communicator.RequestCache
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.protocol.Server
import org.ulalax.playhouse.service.*

open class XApiCommonSender(serviceId: Short,
                            clientCommunicator: ClientCommunicator,
                            reqCache : RequestCache)
    : XSender(serviceId,clientCommunicator,reqCache), ApiCommonSender {

//    override fun updateSession(sessionEndpoint: String,sid:Int,serviceId: Short,sessionInfo:String){
//        val message = Server.UpdateSessionInfoMsg.newBuilder()
//                .setServiceId(serviceId.toInt())
//                .setSessionInfo(sessionInfo).build()
//        sendToBaseSession(sessionEndpoint,sid, Packet(message))
//    }

    override fun accountId(): Long {
        return this.currentHeader?.accountId ?: 0
    }

    override suspend fun createStage(playEndpoint:String, stageType:String, stageId:Long, packet: Packet): CreateStageResult {
        val req = Server.CreateStageReq.newBuilder()
                .setStageType(stageType)
                .setPayloadId(packet.msgId)
                .setPayload(ByteString.copyFrom(packet.data())).build()

        val reply = requestToBaseStage(playEndpoint,stageId,0, Packet(req))
        val res = Server.CreateStageRes.parseFrom(reply.data())
        return CreateStageResult(reply.errorCode, Packet(res.payloadId,res.payload))
    }

    override suspend fun joinStage(playEndpoint: String, stageId: Long,
                                   accountId: Long, sessionEndpoint: String, sid:Int,
                                   packet: Packet
    ): JoinStageResult {
        val req = Server.JoinStageReq.newBuilder()
                .setSessionEndpoint(sessionEndpoint)
                .setSid(sid)
                .setPayloadId(packet.msgId)
                .setPayload(ByteString.copyFrom(packet.data())).build()

        val reply  = requestToBaseStage(playEndpoint,stageId,accountId, Packet(req))
        val res = Server.JoinStageRes.parseFrom(reply.data())

        return JoinStageResult(reply.errorCode,res.stageIdx, Packet(res.payloadId,res.payload))

    }

    override suspend fun createJoinStage(
            playEndpoint: String, stageType: String, stageId: Long,
            createPacket: Packet,
            accountId: Long, sessionEndpoint: String, sid:Int,
            joinPacket: Packet,
    ): CreateJoinStageResult {
        val req = Server.CreateJoinStageReq.newBuilder()
                .setStageType(stageType)
                .setCreatePayloadId(createPacket.msgId)
                .setCreatePayload(ByteString.copyFrom(createPacket.data()))
                .setSessionEndpoint(sessionEndpoint)
                .setSid(sid)
                .setJoinPayloadId(joinPacket.msgId)
                .setJoinPayload(ByteString.copyFrom(joinPacket.data())).build()

        val reply = requestToBaseStage(playEndpoint,stageId,accountId, Packet(req))
        val res = Server.CreateJoinStageRes.parseFrom(reply.data())
        return CreateJoinStageResult(
                reply.errorCode,
                res.isCreated,
                Packet(res.createPayloadId, res.createPayload),
                Packet(res.joinPayloadId, res.joinPayload)
        )

    }
}