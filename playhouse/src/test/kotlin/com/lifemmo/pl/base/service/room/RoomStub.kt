package com.lifemmo.pl.base.service.room

import com.lifemmo.pl.base.protocol.Packet
import com.lifemmo.pl.base.protocol.ReplyPacket
import com.lifemmo.pl.base.service.RoomSender

class UserStub(override val userSender: UserSender) :User {
    override fun onCreate() {
    }

    override fun onDestroy() {
    }

}

class RoomStub(override val roomSender: RoomSender) : Room<UserStub> {
    override suspend fun onCreate(packet: Packet): ReplyPacket {
        return ReplyPacket(0,packet.msgName,packet.movePayload())
    }

    override suspend fun onJoinRoom(user: UserStub, packet: Packet): ReplyPacket {
        return ReplyPacket(0,packet.msgName,packet.movePayload())
    }

    override suspend fun onDispatch(user: UserStub, packet: Packet) {
    }

    override suspend fun onDisconnect(user: UserStub) {
    }

    override suspend fun onPostCreate() {
    }

    override suspend fun onPostJoinRoom(user: UserStub) {
    }
}