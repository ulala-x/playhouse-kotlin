package org.ulalax.playhouse.service.api

import com.google.protobuf.ByteString
import org.ulalax.playhouse.communicator.ClientCommunicator
import org.ulalax.playhouse.communicator.RequestCache
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.protocol.Server
import org.ulalax.playhouse.service.*

abstract class XApiCommonSender(
                            serviceId: Short,
                            clientCommunicator: ClientCommunicator,
                            reqCache : RequestCache)
    : XSender(serviceId,clientCommunicator,reqCache), ApiCommonSender {


//    override fun getAccountId(): Long {
//        return this.currentHeader?.accountId ?: 0
//    }

    override suspend fun createStage(playEndpoint:String, stageType:String, stageId:Long, packet: Packet): CreateStageResult {
        val req = Server.CreateStageReq.newBuilder()
                .setStageType(stageType)
                .setPayloadId(packet.msgId)
                .setPayload(ByteString.copyFrom(packet.data())).build()

        val reply = requestToBaseStage(playEndpoint,stageId,0, Packet(req))
        val res = Server.CreateStageRes.parseFrom(reply.data())
        return CreateStageResult(reply.errorCode, Packet(res.payloadId,res.payload))
    }


}