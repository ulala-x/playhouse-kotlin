package org.ulalax.playhouse.client.network.websocket

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageCodec
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import org.apache.commons.lang3.exception.ExceptionUtils
import org.ulalax.playhouse.THBuffer
import org.ulalax.playhouse.client.network.ByteBufferAllocator
import org.ulalax.playhouse.client.network.PacketParser
import org.ulalax.playhouse.client.network.message.ClientPacket

class WebSocketPacketCodec
    : MessageToMessageCodec< BinaryWebSocketFrame, ClientPacket>() {

    private val parser = PacketParser()
    private val buffer = ByteBufferAllocator.getBuf(1024*8,1024*64*4)
    override fun encode(ctx: ChannelHandlerContext, clientPacket: ClientPacket, out: MutableList<Any>) {
        clientPacket.use {
//            val buffer = ByteBufferAllocator.getBuf()
//            clientPacket.toByteBuf(buffer)
//            out.add(BinaryWebSocketFrame(true,0,buffer))
            val packetSize = clientPacket.data().limit() + PacketParser.HEADER_SIZE - 2
            val buffer = Unpooled.buffer(packetSize)
            clientPacket.toByteBuf(buffer)
            out.add(BinaryWebSocketFrame(true,0, Unpooled.wrappedBuffer(buffer)))
        }
    }

    override fun decode(ctx: ChannelHandlerContext, msg: BinaryWebSocketFrame, out: MutableList<Any>) {
        try {
            buffer.writeBytes(msg.content())
            msg.content().clear()
            val packet = parser.parse(buffer)
            packet.forEach{el->out.add(el)}
        }catch (e:Exception){
            LOG.error(ExceptionUtils.getStackTrace(e),this)
        }
    }
}