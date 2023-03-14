package org.ulalax.playhouse.service.session.network.netty.websocket


import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageCodec
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.logging.log4j.kotlin.logger
import org.ulalax.playhouse.Logger
import org.ulalax.playhouse.communicator.ByteBufferAllocator
import org.ulalax.playhouse.communicator.message.ClientPacket
import org.ulalax.playhouse.service.session.network.PacketParser

class WebSocketPacketCodec(private val log: Logger) : MessageToMessageCodec<BinaryWebSocketFrame, ClientPacket>() {
    private val parser = PacketParser(log)
    override fun encode(ctx: ChannelHandlerContext, clientPacket: ClientPacket, out: MutableList<Any>) {
        clientPacket.use {
            val buffer = ByteBufferAllocator.getBuf()
            clientPacket.toByteBuf(buffer)
            out.add(BinaryWebSocketFrame(true,0,buffer))
        }
    }

    override fun decode(ctx: ChannelHandlerContext, msg: BinaryWebSocketFrame, out: MutableList<Any>) {
        try {
            val packets = parser.parse(msg.content())
            packets.forEach{el->out.add(el)}
        }catch (e:Exception){
            log.error(ExceptionUtils.getStackTrace(e),this::class.simpleName,e)
        }
    }
}