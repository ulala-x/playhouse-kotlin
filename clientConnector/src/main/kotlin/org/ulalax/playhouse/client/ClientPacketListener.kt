package org.ulalax.playhouse.client

import org.ulalax.playhouse.client.network.message.Packet

fun interface ClientPacketListener {
    fun onReceive(targetId: TargetId,packet: Packet)
}