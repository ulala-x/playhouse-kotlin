package org.ulalax.playhouse.base

import com.google.protobuf.ByteString
import com.google.protobuf.GeneratedMessageV3
import io.netty.buffer.*
import java.nio.ByteBuffer

object ByteBufferAllocator {
//    val allocator = UnpooledByteBufAllocator(false)
    val allocator = PooledByteBufAllocator(true)

    //val allocator = PooledByteBufAllocator(true)

    private fun getBuf(defaultBufSize:Int, maxBufSize:Int):ByteBuf  {
        return allocator.buffer(defaultBufSize,maxBufSize)
        //return allocator.directBuffer(defaultBufSize,maxBufSize)
    //    return allocator.directBuffer(defaultBufSize,maxBufSize)
    }
    fun getBuf(size: Int):ByteBuf {return getBuf(size,size) }

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