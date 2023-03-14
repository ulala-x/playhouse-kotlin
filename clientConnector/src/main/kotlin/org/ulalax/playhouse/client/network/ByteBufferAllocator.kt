package org.ulalax.playhouse.client.network

import io.netty.buffer.*

object ByteBufferAllocator {
    private val allocator = UnpooledByteBufAllocator(false)
//    val allocator = PooledByteBufAllocator(true)

    private fun getBuf(defaultBufSize:Int, maxBufSize:Int):ByteBuf  {
        return allocator.buffer(defaultBufSize,maxBufSize)
    }
    fun getBuf(size: Int):ByteBuf {return getBuf(size,size) }
    fun getBuf():ByteBuf{
        return allocator.buffer()
    }
    fun warp(toByteArray: ByteArray): ByteBuf {

        return Unpooled.wrappedBuffer(toByteArray);
    }

//    fun getBuf(message: GeneratedMessageV3):ByteBuf {
//
//        val byteBuf = getBuf(message.serializedSize)
//        val output = ByteBufOutputStream(byteBuf)
//        message.writeTo(output)
//        return byteBuf
//    }
//
//    fun getBuf(raw: ByteArray): ByteBuf {
//
//        val byteBuf: ByteBuf = getBuf(raw.size)
//        byteBuf.writeBytes(raw)
//        return byteBuf
//    }
//
//    fun getBuf(message: ByteString): ByteBuf {
//        val byteBuf = getBuf(message.size())
//        val output = ByteBufOutputStream(byteBuf)
//        message.writeTo(output)
//        return byteBuf
//    }
//
//    fun getCompositeBuffer(): CompositeByteBuf {
//        return this.allocator.compositeBuffer()
//    }
//
//    fun getEmpty(): ByteBuf {
//
//        return this.allocator.buffer()
//    }
//
//    fun getBuf(body: ByteBuffer):ByteBuf {
//        var byteBuf = this.allocator.directBuffer(body.limit())
//        byteBuf.writeBytes(body)
//        return byteBuf
//
//    }


}