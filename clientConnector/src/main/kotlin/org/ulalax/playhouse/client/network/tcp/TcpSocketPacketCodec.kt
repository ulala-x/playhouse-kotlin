package org.ulalax.playhouse.client.network.tcp

import io.netty.buffer.ByteBuf
import org.ulalax.playhouse.protocol.ClientPacket
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageCodec
import io.netty.handler.codec.MessageToMessageCodec
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.logging.log4j.kotlin.logger
import org.ulalax.playhouse.base.WsPacketParser

class TcpSocketPacketCodec : ByteToMessageCodec<ClientPacket>() {
    private val log = logger()
    private val parser = WsPacketParser()
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