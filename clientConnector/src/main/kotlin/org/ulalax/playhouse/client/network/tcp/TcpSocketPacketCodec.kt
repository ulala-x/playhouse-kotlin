package org.ulalax.playhouse.client.network.tcp

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageCodec
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.logging.log4j.kotlin.logger
import org.ulalax.playhouse.client.network.PacketParser
import org.ulalax.playhouse.client.network.message.ClientPacket

class TcpSocketPacketCodec : ByteToMessageCodec<ClientPacket>() {
    private val log = logger()
    private val parser = PacketParser()
    override fun encode(ctx: ChannelHandlerContext, clientPacket: ClientPacket, out: ByteBuf) {
        clientPacket.use {
            clientPacket.toByteBuf(out)
        }
    }

    override fun decode(ctx: ChannelHandlerContext, msg: ByteBuf, out: MutableList<Any>) {
        try {
            val packet = parser.parse(msg)
            packet.forEach{el->out.add(el)}

        }catch (e:Exception){
            log.error(ExceptionUtils.getStackTrace(e))
        }


    }
}