package org.ulalax.playhouse.communicator.message


interface PacketListener {
    fun onReceive(clientPacket: ClientPacket)
}