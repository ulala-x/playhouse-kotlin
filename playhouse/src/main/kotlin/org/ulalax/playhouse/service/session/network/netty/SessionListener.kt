package org.ulalax.playhouse.service.session.network.netty

import io.netty.channel.Channel
import org.ulalax.playhouse.communicator.message.ClientPacket

interface SessionListener {
    fun onConnect(channel: Channel)
    fun onReceive(channel: Channel, clientPacket: ClientPacket)
    fun onDisconnect(channel: Channel)

    fun getSessionId(channel:Channel): Int {
        return Integer.parseInt(channel.id().asLongText().split('-')[2],16)
    }
}