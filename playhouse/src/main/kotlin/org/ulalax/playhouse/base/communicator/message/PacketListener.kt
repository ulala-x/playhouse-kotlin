package org.ulalax.playhouse.base.communicator.message

import org.ulalax.playhouse.base.protocol.ClientPacket


interface PacketListener {
    fun onReceive(clientPacket: ClientPacket)
}