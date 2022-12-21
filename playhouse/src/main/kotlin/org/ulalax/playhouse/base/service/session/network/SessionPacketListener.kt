package org.ulalax.playhouse.base.service.session.network

import org.ulalax.playhouse.base.protocol.ClientPacket
import io.netty.channel.Channel

interface SessionPacketListener {
    fun onConnect(channel: Channel)
    fun onReceive(channel: Channel, clientPacket: ClientPacket)
    fun onDisconnect(channel: Channel)

    fun getSessionId(channel:Channel): Int {
        return Integer.parseInt(channel.id().asLongText().split('-')[2],16)
    }
}