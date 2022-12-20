package com.lifemmo.pl.base.service.room.base.command

import com.lifemmo.pl.base.communicator.message.AsyncBlockPacket
import com.lifemmo.pl.base.communicator.message.RoutePacket
import com.lifemmo.pl.base.service.room.RoomService
import com.lifemmo.pl.base.service.room.base.BaseRoom
import com.lifemmo.pl.base.service.room.base.BaseRoomCmd

class AsyncBlockCmd<T>(override val roomService: RoomService) : BaseRoomCmd {

    override suspend fun execute(baseRoom: BaseRoom, routePacket: RoutePacket) {
        @Suppress("UNCHECKED_CAST")
        val asyncBlock = routePacket as AsyncBlockPacket<Any>
        asyncBlock.asyncPostCallback(asyncBlock.result)
    }
}