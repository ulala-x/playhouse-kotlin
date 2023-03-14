package org.ulalax.playhouse.client.network

import io.netty.channel.Channel
import org.ulalax.playhouse.client.network.message.ClientPacket

interface BasePacketListener {
    fun onConnect(channel: Channel)
    fun onReceive(channel: Channel, clientPacket: ClientPacket)
    fun onDisconnect(channel: Channel)



}