package org.ulalax.playhouse.client.network

import org.ulalax.playhouse.protocol.ClientPacket
import io.netty.channel.Channel

interface BasePacketListener {
    fun onConnect(channel: Channel)
    fun onReceive(channel: Channel, clientPacket: ClientPacket)
    fun onDisconnect(channel: Channel)



}