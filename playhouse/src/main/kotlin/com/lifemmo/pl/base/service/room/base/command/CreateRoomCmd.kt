package com.lifemmo.pl.base.service.room.base.command

import com.google.protobuf.ByteString
import com.lifemmo.pl.base.BaseErrorCode
import com.lifemmo.pl.base.Plbase
import com.lifemmo.pl.base.Plbase.CreateRoomReq
import com.lifemmo.pl.base.communicator.message.RoutePacket
import com.lifemmo.pl.base.protocol.Packet
import com.lifemmo.pl.base.protocol.ReplyPacket
import com.lifemmo.pl.base.service.room.RoomService
import com.lifemmo.pl.base.service.room.base.BaseRoom
import com.lifemmo.pl.base.service.room.base.BaseRoomCmd
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.logging.log4j.kotlin.logger

class CreateRoomCmd(override val roomService: RoomService):BaseRoomCmd {
    private val log = logger()
    override suspend fun execute(baseRoom: BaseRoom, routePacket: RoutePacket) {

        val createRoomReq = CreateRoomReq.parseFrom(routePacket.buffer())
        val packet = Packet(createRoomReq.payloadName,createRoomReq.payload)
        val roomType = createRoomReq.roomType

        try {

            if(!roomService.isValidType(roomType)){
                roomService.errorReply(routePacket.routeHeader,BaseErrorCode.ROOM_TYPE_IS_INVALID)
                return
            }

            val outcome  = baseRoom.create(roomType,packet)
            val roomId = baseRoom.roomId()

            if(!outcome.isSuccess()){
                this.roomService.removeRoom(roomId)
            }

            val res = Plbase.CreateRoomRes.newBuilder()
                .setRoomId(roomId)
                .setPayload(ByteString.copyFrom(outcome.buffer()))
                .setPayloadName(outcome.msgName).build()

            baseRoom.reply(ReplyPacket(outcome.errorCode,res))

            if(outcome.isSuccess()){
                baseRoom.onPostCreate()
            }

        }catch (e:Exception){
            log.error(ExceptionUtils.getStackTrace(e))
        }
    }
}