package com.lifemmo.pl.base.communicator.message

import com.lifemmo.pl.base.protocol.ClientPacket


interface PacketListener {
    fun onReceive(clientPacket: ClientPacket)
}