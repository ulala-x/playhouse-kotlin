package org.ulalax.playhouse.service.session.network.netty.tcp


import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageCodec
import org.apache.commons.lang3.exception.ExceptionUtils
import LOG
import org.ulalax.playhouse.communicator.message.ClientPacket
import org.ulalax.playhouse.service.session.network.PacketParser


class TcpSocketPacketCodec() : ByteToMessageCodec<ClientPacket>() {

    private val parser = PacketParser()
    override fun encode(ctx: ChannelHandlerContext, clientPacket: ClientPacket, out: ByteBuf) {
        clientPacket.use {
            //clientPacket.toByteBuf(out)
            out.writeBytes(clientPacket.payload.data())
        }
    }

    override fun decode(ctx: ChannelHandlerContext, msg: ByteBuf, out: MutableList<Any>) {
        try {
            val packets = parser.parse(msg)
            packets.forEach{el->out.add(el)}
        }catch (e:Exception){
            LOG.error(ExceptionUtils.getStackTrace(e),this,e)
        }
    }
}