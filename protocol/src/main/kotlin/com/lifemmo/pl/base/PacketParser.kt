package com.lifemmo.pl.base

import com.google.protobuf.ByteString
import com.lifemmo.pl.base.protocol.ClientPacket
import com.lifemmo.pl.base.protocol.Payload
import com.lifemmo.pl.base.protocol.ProtoPayload
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.Unpooled
import org.apache.logging.log4j.kotlin.logger
import java.io.InputStream

object PacketParser {
    private val log = logger()
    const val HEADER_SIZE = 256
    const val LENGTH_FIELD_SIZE = 3
    const val MAX_PACKET_SIZE = 65535


    fun parse(buf:ByteBuf): ClientPacket {
        //val packets = arrayListOf<ClientPacket>()

//        try{
            if(buf.readableBytes() < LENGTH_FIELD_SIZE ){
                throw IndexOutOfBoundsException("packet size is too small")
            }
            val headerSize = buf.getUnsignedByte(buf.readerIndex()).toInt()

            if (headerSize > HEADER_SIZE) {
                log.error("Header size over : $headerSize")
                throw IndexOutOfBoundsException("HeaderSizeOver")
            }
            buf.readerIndex(buf.readerIndex()+1)
            val headerInputStream: InputStream = ByteBufInputStream(buf, headerSize)
            val header = Plcommon.HeaderMsg.parseFrom(headerInputStream)
            //buf.readerIndex(buf.readerIndex()+headerSize)
            //val body = ByteString.copyFrom(buf.nioBuffer())

//            if(!buf.isDirect){
//
//            }
//            val body = ByteBufferAllocator.getBuf(buf.nioBuffer());
//            return ClientPacket.of(header, ProtoPayload(body))
            return ClientPacket.of(header, ProtoPayload(buf.retainedSlice()))
            //return ClientPacket.of(header, ProtoPayload(Unpooled.w))

//        }catch (e:Exception){
//            log.error(ExceptionUtils.getStackTrace(e))
//        }


        //return packets

    }

}