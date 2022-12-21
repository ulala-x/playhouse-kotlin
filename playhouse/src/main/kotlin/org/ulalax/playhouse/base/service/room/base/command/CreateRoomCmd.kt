package org.ulalax.playhouse.base.service.room.base.command

import com.google.protobuf.ByteString
import org.ulalax.playhouse.base.communicator.message.RoutePacket
import org.ulalax.playhouse.base.protocol.Packet
import org.ulalax.playhouse.base.protocol.ReplyPacket
import org.ulalax.playhouse.base.service.room.RoomService
import org.ulalax.playhouse.base.service.room.base.BaseRoom
import org.ulalax.playhouse.base.service.room.base.BaseRoomCmd
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.logging.log4j.kotlin.logger
import org.ulalax.playhouse.base.ErrorCode
import org.ulalax.playhouse.base.Server.*

class CreateRoomCmd(override val roomService: RoomService):BaseRoomCmd {
    private val log = logger()
    override suspend fun execute(baseRoom: BaseRoom, routePacket: RoutePacket) {

        val createRoomReq = CreateRoomReq.parseFrom(routePacket.buffer())
        val packet = Packet(createRoomReq.payloadName,createRoomReq.payload)
        val roomType = createRoomReq.roomType

        try {

            if(!roomService.isValidType(roomType)){
                roomService.errorReply(routePacket.routeHeader,
                    ErrorCode.ROOM_TYPE_IS_INVALID)
                return
            }

            val outcome  = baseRoom.create(roomType,packet)
            val roomId = baseRoom.roomId()

            if(!outcome.isSuccess()){
                this.roomService.removeRoom(roomId)
            }

            val res = CreateRoomRes.newBuilder()
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