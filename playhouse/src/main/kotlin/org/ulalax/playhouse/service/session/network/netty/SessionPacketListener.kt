package org.ulalax.playhouse.service.session.network.netty

import org.ulalax.playhouse.protocol.ClientPacket
import io.netty.channel.Channel

interface SessionPacketListener {
    fun onConnect(channel: Channel)
    fun onReceive(channel: Channel, clientPacket: ClientPacket)
    fun onDisconnect(channel: Channel)

    fun getSessionId(channel:Channel): Int {
        return Integer.parseInt(channel.id().asLongText().split('-')[2],16)
    }
}