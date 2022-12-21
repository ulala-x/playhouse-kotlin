package org.ulalax.playhouse.service.play.contents

import org.ulalax.playhouse.protocol.Packet


interface PacketCmd<R,U> {
    suspend fun execute(room: R, user: U, packet: Packet)
}