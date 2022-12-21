package org.ulalax.playhouse.base.service.room.contents

import org.ulalax.playhouse.base.protocol.Packet


interface PacketCmd<R,U> {
    suspend fun execute(room: R, user: U, packet: Packet)
}