package org.ulalax.playhouse.service.session.network.Stream

import org.ulalax.playhouse.protocol.ClientPacket
import io.netty.channel.Channel

interface SessionPacketListener {
    fun onConnect(sid: Int)
    fun onReceive(sid: Int, clientPacket: ClientPacket)
    fun onDisconnect(sid: Int)

//    fun getSessionId(channel:Channel): Int {
//        return Integer.parseInt(channel.id().asLongText().split('-')[2],16)
//    }
}