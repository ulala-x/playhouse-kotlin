package org.ulalax.playhouse.base.service.api

import com.google.protobuf.ByteString
import org.ulalax.playhouse.base.communicator.CommunicateClient
import org.ulalax.playhouse.base.protocol.Packet
import org.ulalax.playhouse.base.service.*
import org.apache.logging.log4j.kotlin.logger
import org.ulalax.playhouse.base.Server.*

open class ApiBaseSenderImpl(serviceId:String,
                             communicateClient: CommunicateClient,
                             reqCache:RequestCache): BaseSenderImpl(serviceId,communicateClient,reqCache),ApiBaseSender{
    override fun updateSession(sessionEndpoint: String,sid:Int,serviceId: String,sessionInfo:String){
        val message = UpdateSessionInfoMsg.newBuilder()
            .setServiceId(serviceId)
            .setSessionInfo(sessionInfo).build()
        sendToBaseSession(sessionEndpoint,sid,Packet(message))
    }

    override fun createRoom(roomEndpoint:String,roomType:String,packet: Packet): CreateRoomResult {
        val req = CreateRoomReq.newBuilder()
            .setRoomType(roomType)
            .setPayloadName(packet.msgName)
            .setPayload(ByteString.copyFrom(packet.buffer())).build()

        val reply = callToBaseRoom(roomEndpoint,0,0,Packet(req))
        val res = CreateRoomRes.parseFrom(reply.buffer())
        return CreateRoomResult(reply.errorCode,res.roomId,Packet(res.payloadName,res.payload))
    }

    override fun joinRoom(roomEndpoint: String, roomId: Long,
                          accountId: Long,sessionEndpoint: String,sid:Int,
                          packet: Packet): JoinRoomResult {
        val req = JoinRoomReq.newBuilder()
            .setSessionEndpoint(sessionEndpoint)
            .setSid(sid)
            .setPayloadName(packet.msgName)
            .setPayload(ByteString.copyFrom(packet.buffer())).build()

        val reply  = callToBaseRoom(roomEndpoint,roomId,accountId,Packet(req))
        val res = JoinRoomRes.parseFrom(reply.buffer())

        return JoinRoomResult(reply.errorCode,Packet(res.payloadName,res.payload))

    }

    override fun createJoinRoom(
        roomEndpoint: String, roomType: String, roomId: Long,
        createPacket: Packet,
        accountId: Long,sessionEndpoint: String,sid:Int,
        joinPacket: Packet,
    ): CreateJoinRoomResult {
        val req = CreateJoinRoomReq.newBuilder()
            .setRoomType(roomType)
            .setCreatePayloadName(createPacket.msgName)
            .setCreatePayload(ByteString.copyFrom(createPacket.buffer()))
            .setSessionEndpoint(sessionEndpoint)
            .setSid(sid)
            .setJoinPayloadName(joinPacket.msgName)
            .setJoinPayload(ByteString.copyFrom(joinPacket.buffer())).build()

        val reply = callToBaseRoom(roomEndpoint,roomId,accountId,Packet(req))
        val res = CreateJoinRoomRes.parseFrom(reply.buffer())
        return CreateJoinRoomResult(
            reply.errorCode,
            res.isCreated,
            Packet(res.createPayloadName,res.createPayload),
            Packet(res.joinPayloadName,res.joinPayload))

    }
}

class ApiSenderImpl (private val serviceId:String,
                     private val communicateClient: CommunicateClient,
                     private val reqCache:RequestCache) : ApiBaseSenderImpl(serviceId,communicateClient,reqCache),
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
            sendToBaseSession(from,sid,Packet(message))
        } ?: throw ApiException.NotExistApiHeaderInfoException()

    }


    fun clone(): ApiSenderImpl {
        return ApiSenderImpl(this.serviceId,this.communicateClient,this.reqCache)
    }

}