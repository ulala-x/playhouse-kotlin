package org.ulalax.playhouse.client.network

import io.netty.buffer.ByteBuf
import org.apache.commons.lang3.exception.ExceptionUtils
import org.ulalax.playhouse.XBitConverter
import org.ulalax.playhouse.client.network.message.BytePayload
import org.ulalax.playhouse.client.network.message.ClientPacket
import org.ulalax.playhouse.client.network.message.Header
import java.io.IOException

open class PacketParser {
    companion object {
        const val MAX_PACKET_SIZE = 65535
        const val HEADER_SIZE = 10
    }

    open fun parse(buf:ByteBuf): ArrayDeque<ClientPacket> {
        val packets = ArrayDeque<ClientPacket>()

        while (buf.readableBytes() >= HEADER_SIZE) {
            try {

                val bodySize = buf.getUnsignedShort(buf.readerIndex())

                if (bodySize > MAX_PACKET_SIZE) {
                    LOG.error("body size over : $bodySize",this)
                    throw IOException("BodySizeOver")
                }

                if (buf.readableBytes() < HEADER_SIZE + bodySize) {
                    return packets
                }

                buf.readUnsignedShort()

                val serviceId = buf.readShort()
                val msgId = buf.readInt()
                val msgSeq = buf.readShort()
                val errorCode = buf.readShort()

                val body = ByteBufferAllocator.getBuf(bodySize)

                buf.readBytes(body,0,bodySize)
                body.writerIndex(bodySize)


                val clientPacket = ClientPacket.of(Header(serviceId,msgId,msgSeq, errorCode), BytePayload(body))
                packets.add(clientPacket)
            }catch (e:Exception){
                LOG.error(ExceptionUtils.getStackTrace(e),this,e)
            }
        }
        return packets
    }
}