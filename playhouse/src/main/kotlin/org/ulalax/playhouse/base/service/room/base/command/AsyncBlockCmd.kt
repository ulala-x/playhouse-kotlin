package org.ulalax.playhouse.base.service.room.base.command

import org.ulalax.playhouse.base.communicator.message.AsyncBlockPacket
import org.ulalax.playhouse.base.communicator.message.RoutePacket
import org.ulalax.playhouse.base.service.room.RoomService
import org.ulalax.playhouse.base.service.room.base.BaseRoom
import org.ulalax.playhouse.base.service.room.base.BaseRoomCmd

class AsyncBlockCmd<T>(override val roomService: RoomService) : BaseRoomCmd {

    override suspend fun execute(baseRoom: BaseRoom, routePacket: RoutePacket) {
        @Suppress("UNCHECKED_CAST")
        val asyncBlock = routePacket as AsyncBlockPacket<Any>
        asyncBlock.asyncPostCallback(asyncBlock.result)
    }
}