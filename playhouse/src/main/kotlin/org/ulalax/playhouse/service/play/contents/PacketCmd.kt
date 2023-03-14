package org.ulalax.playhouse.service.play.contents

import org.ulalax.playhouse.communicator.message.Packet


interface PacketCmd<R,U> {
    suspend fun execute(room: R, user: U, packet: Packet)
}