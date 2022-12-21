package org.ulalax.playhouse.base.service.room.base.command

import com.google.protobuf.ByteString
import org.ulalax.playhouse.base.communicator.message.RoutePacket
import org.ulalax.playhouse.base.protocol.Packet
import org.ulalax.playhouse.base.protocol.ReplyPacket
import org.ulalax.playhouse.base.service.room.RoomService
import org.ulalax.playhouse.base.service.room.base.BaseRoom
import org.ulalax.playhouse.base.service.room.base.BaseRoomCmd
import org.apache.logging.log4j.kotlin.logger
import org.ulalax.playhouse.base.Server.*

class JoinRoomCmd(override val roomService: RoomService):BaseRoomCmd {
    private val log = logger()
    override suspend fun execute(baseRoom: BaseRoom, routePacket: RoutePacket) {

        val request = JoinRoomReq.parseFrom(routePacket.buffer())
        val accountId = routePacket.accountId()
        val sessionEndpoint = request.sessionEndpoint
        val sid = request.sid
        val packet = Packet(request.payloadName,request.payload)
        val apiEndpoint = routePacket.routeHeader.from


        val outcome = baseRoom.join(accountId,sessionEndpoint,sid,apiEndpoint,packet)
        val response = JoinRoomRes.newBuilder()
            .setPayload(ByteString.copyFrom(outcome.buffer()))
            .setPayloadName(outcome.msgName)
            .build()

        baseRoom.reply(ReplyPacket(outcome.errorCode,response))

        if(outcome.isSuccess()){
            baseRoom.onPostJoinRoom(accountId)
        }

    }
}