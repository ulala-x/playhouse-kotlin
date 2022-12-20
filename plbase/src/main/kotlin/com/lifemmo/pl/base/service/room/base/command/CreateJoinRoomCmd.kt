package com.lifemmo.pl.base.service.room.base.command

import com.google.protobuf.ByteString
import com.lifemmo.pl.base.BaseErrorCode
import com.lifemmo.pl.base.Plbase
import com.lifemmo.pl.base.communicator.message.RoutePacket
import com.lifemmo.pl.base.protocol.Packet
import com.lifemmo.pl.base.protocol.ReplyPacket
import com.lifemmo.pl.base.service.room.RoomService
import com.lifemmo.pl.base.service.room.base.BaseRoom
import com.lifemmo.pl.base.service.room.base.BaseRoomCmd

class CreateJoinRoomCmd(override val roomService: RoomService) : BaseRoomCmd {
    override suspend fun execute(baseRoom: BaseRoom, routePacket: RoutePacket) {
        val request = Plbase.CreateJoinRoomReq.parseFrom(routePacket.buffer())
        val createRoomPacket = Packet(request.createPayloadName,request.createPayload)
        val roomType = request.roomType
        val joinRoomPacket = Packet(request.joinPayloadName,request.joinPayload)
        val accountId = routePacket.accountId()
        val roomId = routePacket.roomId()
        val sessionEndpoint = request.sessionEndpoint
        val sid = request.sid
        val apiEndpoint = routePacket.routeHeader.from

        var createReply: ReplyPacket
        val responseBuilder = Plbase.CreateJoinRoomRes.newBuilder()

        if(!roomService.isValidType(roomType)){
            roomService.errorReply(routePacket.routeHeader, BaseErrorCode.ROOM_TYPE_IS_INVALID)
            return
        }

        if(!baseRoom.isCreated){
            createReply = baseRoom.create(roomType,createRoomPacket)
            responseBuilder
                .setCreatePayloadName(createReply.msgName)
                .setCreatePayload(ByteString.copyFrom(createReply.buffer()))

            if(!createReply.isSuccess()){
                roomService.removeRoom(roomId)
                val response = Plbase.CreateJoinRoomRes.newBuilder()
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