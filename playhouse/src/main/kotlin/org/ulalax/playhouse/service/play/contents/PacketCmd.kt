package org.ulalax.playhouse.service.play.contents

import org.ulalax.playhouse.communicator.message.Packet


interface PacketCmd<S,A> {
    suspend fun execute(stage: S, actor: A, packet: Packet)
}