package org.ulalax.playhouse.service.session.network.netty.tcp


import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageCodec
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.logging.log4j.kotlin.logger
import org.ulalax.playhouse.Logger
import org.ulalax.playhouse.communicator.message.ClientPacket
import org.ulalax.playhouse.service.session.network.PacketParser


class TcpSocketPacketCodec(private val log: Logger) : ByteToMessageCodec<ClientPacket>() {

    private val parser = PacketParser(log)
    override fun encode(ctx: ChannelHandlerContext, clientPacket: ClientPacket, out: ByteBuf) {
        clientPacket.use {
            clientPacket.toByteBuf(out)
        }
    }

    override fun decode(ctx: ChannelHandlerContext, msg: ByteBuf, out: MutableList<Any>) {
        try {
            val packets = parser.parse(msg)
            packets.forEach{el->out.add(el)}
        }catch (e:Exception){
            log.error(ExceptionUtils.getStackTrace(e),this::class.simpleName,e)
        }
    }
}