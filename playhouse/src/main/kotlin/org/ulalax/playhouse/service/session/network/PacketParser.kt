package org.ulalax.playhouse.service.session.network

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import org.apache.commons.lang3.exception.ExceptionUtils
import org.ulalax.playhouse.Logger
import org.ulalax.playhouse.communicator.message.ClientPacket
import org.ulalax.playhouse.communicator.message.XPayload
import org.ulalax.playhouse.protocol.Common
import org.zeromq.ZFrame
import java.io.InputStream

open class PacketParser(private val log:Logger) {

    companion object {
        const val MAX_PACKET_SIZE = 65535
        const val HEADER_SIZE = 256
        const val LENGTH_FIELD_SIZE = 3
    }

    open fun parse(buf:ByteBuf): ArrayDeque<ClientPacket> {
        val packets = ArrayDeque<ClientPacket>()

        while (buf.isReadable) {
            try {
                if (buf.readableBytes() < LENGTH_FIELD_SIZE) {
                    break
                }
                val headerSize = buf.getUnsignedByte(buf.readerIndex()).toInt()

                if (headerSize > HEADER_SIZE) {
                    log.error("Header size over : $headerSize",this::class.simpleName)
                    throw IndexOutOfBoundsException("HeaderSizeOver")
                }

                val bodySize = buf.getUnsignedShort(buf.readerIndex()+1);
                if (bodySize > MAX_PACKET_SIZE) {
                    log.error("body size over : $headerSize",this::class.simpleName)
                    throw IndexOutOfBoundsException("BodySizeOver")
                }

                //buffer 에 남아있는 data 크기가 패킷 사이즈만큼 안되면
                if(bodySize + LENGTH_FIELD_SIZE > buf.readableBytes()){
                    break;
                }

                buf.readerIndex(buf.readerIndex() + 1 + 2)

                val headerInputStream: InputStream = ByteBufInputStream(buf, headerSize)
                val header = Common.HeaderMsg.parseFrom(headerInputStream)

                val bodyInputStream = ByteBufInputStream(buf, bodySize)
                val body = bodyInputStream.readAllBytes()

                val clientPacket = ClientPacket.of(header, XPayload(ZFrame(body)))
                packets.add(clientPacket)
            }catch (e:Exception){
                log.error(ExceptionUtils.getStackTrace(e),this::class.simpleName,e)
            }
        }
        return packets
    }
}