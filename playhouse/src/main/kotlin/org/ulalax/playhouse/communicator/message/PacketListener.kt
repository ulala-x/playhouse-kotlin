package org.ulalax.playhouse.communicator.message

import org.ulalax.playhouse.protocol.ClientPacket


interface PacketListener {
    fun onReceive(clientPacket: ClientPacket)
}