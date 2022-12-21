package com.lifemmo.pl.base.service.room.base.command

import com.google.protobuf.ByteString
import com.lifemmo.pl.base.Plbase
import com.lifemmo.pl.base.communicator.message.RoutePacket
import com.lifemmo.pl.base.protocol.Packet
import com.lifemmo.pl.base.protocol.ReplyPacket
import com.lifemmo.pl.base.service.room.RoomService
import com.lifemmo.pl.base.service.room.base.BaseRoom
import com.lifemmo.pl.base.service.room.base.BaseRoomCmd
import org.apache.logging.log4j.kotlin.logger

class JoinRoomCmd(override val roomService: RoomService):BaseRoomCmd {
    private val log = logger()
    override suspend fun execute(baseRoom: BaseRoom, routePacket: RoutePacket) {

        val request = Plbase.JoinRoomReq.parseFrom(routePacket.buffer())
        val accountId = routePacket.accountId()
        val sessionEndpoint = request.sessionEndpoint
        val sid = request.sid
        val packet = Packet(request.payloadName,request.payload)
        val apiEndpoint = routePacket.routeHeader.from


        val outcome = baseRoom.join(accountId,sessionEndpoint,sid,apiEndpoint,packet)
        val response = Plbase.JoinRoomRes.newBuilder()
            .setPayload(ByteString.copyFrom(outcome.buffer()))
            .setPayloadName(outcome.msgName)
            .build()

        baseRoom.reply(ReplyPacket(outcome.errorCode,response))

        if(outcome.isSuccess()){
            baseRoom.onPostJoinRoom(accountId)
        }

    }
}