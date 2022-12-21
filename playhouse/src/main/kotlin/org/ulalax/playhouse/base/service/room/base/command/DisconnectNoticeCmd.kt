package org.ulalax.playhouse.base.service.room.base.command

import org.ulalax.playhouse.base.communicator.message.RoutePacket
import org.ulalax.playhouse.base.service.room.RoomService
import org.ulalax.playhouse.base.service.room.base.BaseRoom
import org.ulalax.playhouse.base.service.room.base.BaseRoomCmd

class DisconnectNoticeCmd(override val roomService: RoomService) : BaseRoomCmd {
    override suspend fun execute(baseRoom: BaseRoom, routePacket: RoutePacket) {
        val accountId = routePacket.accountId()
        baseRoom.onDisconnect(accountId)
    }
}