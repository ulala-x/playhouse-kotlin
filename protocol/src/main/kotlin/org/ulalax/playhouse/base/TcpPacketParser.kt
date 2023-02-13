package org.ulalax.playhouse.base

import io.netty.buffer.ByteBuf
import io.netty.buffer.PooledByteBufAllocator
import io.netty.buffer.Unpooled
import org.ulalax.playhouse.protocol.ClientPacket

class TcpPacketParser : PacketParser() {

//    private var buffer:ByteBuf
//
//    private fun newBuffer():ByteBuf{
//        return ByteBufferAllocator.getBuf(PacketParser.MAX_PACKET_SIZE)
//    }
//
//    init {
//        buffer = newBuffer()
//    }

    override fun parse(data: ByteBuf): ArrayDeque<ClientPacket> {

//        val dataSize = data.readableBytes()
//
//        val writableSize = buffer.capacity() - buffer.writerIndex()
//
//        if(dataSize > writableSize){
//            var requiredSize = buffer.readableBytes() + dataSize
//            if( requiredSize > buffer.capacity()){
//                buffer.capacity(requiredSize)
//            }
//            buffer.writeBytes(buffer,buffer.readerIndex(),buffer.readableBytes())
//        }
//
//        buffer.writeBytes(data)

        return super.parse(data)
    }
}