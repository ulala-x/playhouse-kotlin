package org.ulalax.playhouse.service.session.network

import io.netty.buffer.ByteBuf
import org.apache.commons.lang3.exception.ExceptionUtils
import LOG
import org.ulalax.playhouse.THBuffer
import org.ulalax.playhouse.communicator.ConstOption.HEADER_SIZE
import org.ulalax.playhouse.communicator.ConstOption.MAX_PACKET_SIZE
import org.ulalax.playhouse.communicator.message.ClientPacket
import org.ulalax.playhouse.communicator.message.FramePayload
import org.ulalax.playhouse.communicator.message.Header
import org.zeromq.ZFrame
import java.io.IOException


open class PacketParser {



    open fun parse(buf: ByteBuf): ArrayDeque<ClientPacket> {
        val packets = ArrayDeque<ClientPacket>()

        while (buf.readableBytes() >= HEADER_SIZE) {
            try {

                val bodySize = buf.getShort(buf.readerIndex())

//                if (bodySize > MAX_PACKET_SIZE) {
//                    LOG.error("body size over : $bodySize",this)
//                    throw IOException("BodySizeOver")
//                }

                if (buf.readableBytes() < HEADER_SIZE + bodySize) {
                    return packets
                }

                buf.readShort()

                val serviceId = buf.readShort()
                val msgId = buf.readInt()
                val msgSeq = buf.readShort()
                val stageIndex = buf.readByte()

                val bodyBuffer = THBuffer.buffer()
                buf.readBytes(bodyBuffer,bodySize.toInt())
                val body = ZFrame(bodyBuffer.array(),bodyBuffer.arrayOffset()+bodyBuffer.readerIndex(),bodySize.toInt())

                LOG.info("${body.data()}",this);

                val clientPacket = ClientPacket.of(Header(serviceId,msgId,msgSeq, 0,stageIndex.toUByte()), FramePayload(body))
                packets.add(clientPacket)
            }catch (e:Exception){
                LOG.error(ExceptionUtils.getStackTrace(e),this,e)
            }
        }
        return packets
    }
}