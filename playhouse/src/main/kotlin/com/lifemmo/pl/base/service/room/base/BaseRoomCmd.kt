package com.lifemmo.pl.base.service.room.base

import com.lifemmo.pl.base.communicator.message.RoutePacket
import com.lifemmo.pl.base.service.room.RoomSenderImpl
import com.lifemmo.pl.base.service.room.RoomService
import com.lifemmo.pl.base.service.room.base.BaseRoom

interface BaseRoomCmd {
    val roomService: RoomService
    suspend fun execute(baseRoom: BaseRoom, routePacket: RoutePacket)
}