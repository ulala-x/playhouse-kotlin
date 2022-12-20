package com.lifemmo.client.netty

import com.lifemmo.pl.base.protocol.ClientPacket
import io.netty.channel.Channel

interface BasePacketListener {
    fun onConnect(channel: Channel)
    fun onReceive(channel: Channel, clientPacket: ClientPacket)
    fun onDisconnect(channel: Channel)

    fun getSessionId(channel:Channel): Int {
        return Integer.parseInt(channel.id().asLongText().split('-')[2],16)
    }


}