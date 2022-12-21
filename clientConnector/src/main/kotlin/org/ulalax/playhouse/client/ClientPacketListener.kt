package org.ulalax.playhouse.client

import org.ulalax.playhouse.base.protocol.Packet


fun interface ClientPacketListener {
    fun onReceive(serviceId: String, packet: Packet)
}