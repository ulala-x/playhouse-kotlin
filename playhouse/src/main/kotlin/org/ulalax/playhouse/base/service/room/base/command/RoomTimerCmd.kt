package org.ulalax.playhouse.base.service.room.base.command

import org.ulalax.playhouse.base.communicator.message.RoutePacket
import org.ulalax.playhouse.base.service.room.RoomService
import org.ulalax.playhouse.base.service.room.base.BaseRoom
import org.ulalax.playhouse.base.service.room.base.BaseRoomCmd
import org.apache.logging.log4j.kotlin.logger

class RoomTimerCmd(override val roomService: RoomService):BaseRoomCmd {
    private val log = logger()
    override suspend fun execute(baseRoom: BaseRoom, routePacket: RoutePacket) {

        val timerCallback = routePacket.timerCallback
        val timerId = routePacket.timerId
        if(baseRoom.hasTimer(timerId)){
            timerCallback()
        }else{
            log.warn("timer already canceled roomId:${baseRoom.roomId()}, timerId:$timerId")
        }
    }


}