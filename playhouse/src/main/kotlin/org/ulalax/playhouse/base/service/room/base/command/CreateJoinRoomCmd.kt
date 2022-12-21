package org.ulalax.playhouse.base.service.room.base.command

import com.google.protobuf.ByteString
import org.ulalax.playhouse.base.ErrorCode
import org.ulalax.playhouse.base.Server
import org.ulalax.playhouse.base.Server.*
import org.ulalax.playhouse.base.communicator.message.RoutePacket
import org.ulalax.playhouse.base.protocol.Packet
import org.ulalax.playhouse.base.protocol.ReplyPacket
import org.ulalax.playhouse.base.service.room.RoomService
import org.ulalax.playhouse.base.service.room.base.BaseRoom
import org.ulalax.playhouse.base.service.room.base.BaseRoomCmd

class CreateJoinRoomCmd(override val roomService: RoomService) : BaseRoomCmd {
    override suspend fun execute(baseRoom: BaseRoom, routePacket: RoutePacket) {
        val request = CreateJoinRoomReq.parseFrom(routePacket.buffer())
        val createRoomPacket = Packet(request.createPayloadName,request.createPayload)
        val roomType = request.roomType
        val joinRoomPacket = Packet(request.joinPayloadName,request.joinPayload)
        val accountId = routePacket.accountId()
        val roomId = routePacket.roomId()
        val sessionEndpoint = request.sessionEndpoint
        val sid = request.sid
        val apiEndpoint = routePacket.routeHeader.from

        var createReply: ReplyPacket
        val responseBuilder = CreateJoinRoomRes.newBuilder()

        if(!roomService.isValidType(roomType)){
            roomService.errorReply(routePacket.routeHeader, ErrorCode.ROOM_TYPE_IS_INVALID)
            return
        }

        if(!baseRoom.isCreated){
            createReply = baseRoom.create(roomType,createRoomPacket)
            responseBuilder
                .setCreatePayloadName(createReply.msgName)
                .setCreatePayload(ByteString.copyFrom(createReply.buffer()))

            if(!createReply.isSuccess()){
                roomService.removeRoom(roomId)
                val response = CreateJoinRoomRes.newBuilder()
                                    .setCreatePayloadName(createReply.msgName)
                                    .setCreatePayload(ByteString.copyFrom(createReply.buffer())).build()

                baseRoom.reply(ReplyPacket(createReply.errorCode,response))
                return
            }else{
                baseRoom.onPostCreate()
                responseBuilder.isCreated = true
            }

        }



        val joinReply = baseRoom.join(accountId,sessionEndpoint,sid,apiEndpoint,joinRoomPacket)
        val response = responseBuilder
            .setJoinPayloadName(joinReply.msgName)
            .setJoinPayload(ByteString.copyFrom(joinReply.buffer()))
            .build()

        baseRoom.reply(ReplyPacket(joinReply.errorCode,response))

        if(joinReply.isSuccess()){
            baseRoom.onPostJoinRoom(accountId)
        }

    }
}