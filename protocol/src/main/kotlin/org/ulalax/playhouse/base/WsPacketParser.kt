package org.ulalax.playhouse.base

import io.netty.buffer.ByteBuf
import org.ulalax.playhouse.protocol.ClientPacket

class WsPacketParser : PacketParser() {

    override fun parse(data: ByteBuf): ArrayDeque<ClientPacket> {
        return super.parse(data)
    }
}