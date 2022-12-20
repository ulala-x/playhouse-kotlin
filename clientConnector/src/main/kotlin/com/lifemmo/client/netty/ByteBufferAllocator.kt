package com.lifemmo.client.netty

import com.google.protobuf.GeneratedMessageV3
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufOutputStream
import io.netty.buffer.UnpooledByteBufAllocator

object ByteBufferAllocator {
    val allocator = UnpooledByteBufAllocator(false)

    private fun getBuf(defaultBufSize:Int, maxBufSize:Int):ByteBuf  { return allocator.buffer(defaultBufSize,maxBufSize) }
    private fun getBuf(size: Int):ByteBuf {return getBuf(size,size) }

    fun getBuf(message: GeneratedMessageV3):ByteBuf {
        val byteBuf = getBuf(message.serializedSize)
        val output = ByteBufOutputStream(byteBuf)
        message.writeTo(output)
        return byteBuf
    }

    fun getBuf(raw: ByteArray): ByteBuf {
        val byteBuf: ByteBuf = getBuf(raw.size)
        byteBuf.writeBytes(raw)
        return byteBuf
    }


}