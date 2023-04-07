package org.ulalax.playhouse

import io.netty.buffer.ByteBuf
import io.netty.buffer.PooledByteBufAllocator
import io.netty.buffer.Unpooled


object THBuffer{
    private val thBuffer = ThreadLocal<ByteBuf>()

    fun buffer() : ByteBuf {
        if(thBuffer.get() == null){
            thBuffer.set(Unpooled.buffer(1024*64))
        }
        val buffer = thBuffer.get();
        buffer.clear()
        return buffer
    }
}