package org.ulalax.playhouse.base.service.room.base

import org.ulalax.playhouse.base.communicator.message.RoutePacket
import org.ulalax.playhouse.base.service.room.RoomSenderImpl
import org.ulalax.playhouse.base.service.room.RoomService
import org.ulalax.playhouse.base.service.room.base.BaseRoom

interface BaseRoomCmd {
    val roomService: RoomService
    suspend fun execute(baseRoom: BaseRoom, routePacket: RoutePacket)
}