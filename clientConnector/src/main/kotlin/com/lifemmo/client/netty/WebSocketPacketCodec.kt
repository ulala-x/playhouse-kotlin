package com.lifemmo.client.netty

import com.lifemmo.pl.base.ByteBufferAllocator
import com.lifemmo.pl.base.PacketParser
import com.lifemmo.pl.base.protocol.ClientPacket
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageCodec
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.logging.log4j.kotlin.logger

class WebSocketPacketCodec : MessageToMessageCodec< BinaryWebSocketFrame,ClientPacket>() {
    private val log = logger()
    override fun encode(ctx: ChannelHandlerContext, msg: ClientPacket, out: MutableList<Any>) {
//        val buffer = ByteBufferAllocator.allocator.buffer()
//        buffer.writeBytes(msg.toByteArray())
//        out.add(buffer)
//        val buffer = ByteBufferAllocator.allocator.buffer()
//        val header = msg.header.toMsg()
//        val headerSize = header.serializedSize
//        val body = msg.buffer
//        buffer.writeByte(headerSize)
//        buffer.writeBytes(header.toByteArray())
//        buffer.writeBytes(body.nioBuffer().array())
//        out.add(buffer)
    }

    override fun decode(ctx: ChannelHandlerContext, msg: BinaryWebSocketFrame, out: MutableList<Any>) {
//        val clientPacket = ClientPacket.messageOf(PacketMsg.parseFrom(msg.content().nioBuffer()))
//        out.add(clientPacket)
        try {
            val packet = PacketParser.parse(msg.content())
            out.add(packet)
        }catch (e:Exception){
            log.error(ExceptionUtils.getStackTrace(e))
        }


    }
}