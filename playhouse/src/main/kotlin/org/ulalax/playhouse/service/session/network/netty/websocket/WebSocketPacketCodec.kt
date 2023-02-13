package org.ulalax.playhouse.service.session.network.netty.websocket


import org.ulalax.playhouse.protocol.ClientPacket
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageCodec
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.logging.log4j.kotlin.logger
import org.ulalax.playhouse.base.WsPacketParser

class WebSocketPacketCodec : MessageToMessageCodec<BinaryWebSocketFrame, ClientPacket>() {
    private val log = logger()
    private val parser = WsPacketParser()
    override fun encode(ctx: ChannelHandlerContext, clientPacket: ClientPacket, out: MutableList<Any>) {
        clientPacket.use {
            out.add(BinaryWebSocketFrame(clientPacket.toByteBuf()))
        }
    }

    override fun decode(ctx: ChannelHandlerContext, msg: BinaryWebSocketFrame, out: MutableList<Any>) {
        try {
            try {
                val packets = parser.parse(msg.content())
                packets.forEach{el->out.add(el)}
            }catch (e:Exception){
                log.error(ExceptionUtils.getStackTrace(e))
            }
        }catch (e:Exception){
            log.error(ExceptionUtils.getStackTrace(e))
        }
    }
}