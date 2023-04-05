package org.ulalax.playhouse

import io.netty.buffer.ByteBuf
import io.netty.buffer.PooledByteBufAllocator

class RingBuffer(capacity :Int, private val maxCapacity: Int = capacity) {
    private var buffer: ByteBuf = PooledByteBufAllocator.DEFAULT.heapBuffer(capacity).retain()
    private var readIndex = 0
    private var writeIndex = 0
    private var size = 0

    val capacity: Int
        get() = buffer.capacity()
    val count: Int
        get() = size

    fun enqueue(item: Byte) {
        if (size == buffer.capacity()) {
            resizeBuffer(buffer.capacity() * 2)
        }

        buffer.setByte(writeIndex, item.toInt())
        writeIndex = nextIndex(writeIndex)

        size++
    }

    fun enqueue(data: ByteArray) {
        data.forEach { enqueue(it) }
    }

    private fun resizeBuffer(newCapacity: Int) {
        if (newCapacity > maxCapacity) {
            throw IllegalStateException("Ring buffer has reached maximum capacity")
        }
        val newBuffer = PooledByteBufAllocator.DEFAULT.heapBuffer(newCapacity)
        writeIndex = size
        while (size != 0) {
            newBuffer.writeByte(dequeue().toInt())
        }
        buffer.release()
        buffer = newBuffer;
//        buffer.clear()
//        buffer.writerIndex(0)
//        buffer.readerIndex(0)
//        buffer.capacity(newCapacity)
//        buffer.writeBytes(newBuffer)
//        newBuffer.release()
        readIndex = 0
        size = writeIndex;
        readIndex = 0;
    }

    fun dequeue(): Byte {
        if (size == 0) {
            throw IllegalStateException("Ring buffer is empty")
        }
        val item = buffer.getByte(readIndex)
        buffer.setByte(readIndex, 0)
        readIndex = nextIndex(readIndex)
        size--
        return item.toByte()
    }

    private fun nextIndex(index: Int): Int {
        return (index + 1) % buffer.capacity()
    }

    fun peek(): Byte {
        if (size == 0) {
            throw IllegalStateException("Ring buffer is empty")
        }
        return buffer.getByte(readIndex)
    }

    fun clear() {
        readIndex = 0
        writeIndex = 0
        size = 0
    }

    fun clear(count: Int) {
        if (count > size) {
            throw IllegalArgumentException("Cannot clear more items than the current size of the ring buffer")
        }
        repeat(count) {
            readIndex = nextIndex(readIndex)
        }
        size -= count
    }

    fun read(buffer: ByteArray, offset: Int, count: Int): Int {
        var bytesRead = 0
        while (bytesRead < count && size > 0) {
            buffer[offset + bytesRead] = dequeue()
            bytesRead++
        }
        return bytesRead
    }

    fun write(buffer: ByteArray, offset: Int, count: Int) {
        repeat(count) {
            enqueue(buffer[offset + it])
        }
    }

    private fun isReadIndexValid(index: Int): Boolean {
        return if (size == 0) {
            false
        } else if (writeIndex > readIndex) {
            index in readIndex until writeIndex
        } else if (writeIndex < readIndex) {
            index >= readIndex || index < writeIndex
        } else {
            // writeIndex와 readIndex가 같은 경우
            // writeIndex와 readIndex가 같아진 직후에는 dequeue 작업을 하지 않은 경우이므로, 값 get이 불가능합니다.
            true
        }
    }
    private fun isReadIndexValid(index: Int, size: Int): Boolean {
        var valueIndex = index
        for( i in 0 until size ) {
            if( !isReadIndexValid(valueIndex)){
                return false;
            }
            valueIndex = nextIndex(index)
        }
        return true
    }

    private fun moveIndex(index: Int, count: Int): Int {
        var newIndex = index;
        for( i in 0 until count){
            newIndex = nextIndex(newIndex)
        }
        return newIndex
    }

    private fun getInt16(index: Int): Short {
        return ((buffer.getByte(index).toUByte().toInt() shl 8) or buffer.getByte(nextIndex(index)).toUByte().toInt()).toShort()
    }

    private fun getInt32(index: Int): Int {
//        return ((buffer.getByte(index).toInt() shl 24) or
//                (buffer.getByte(nextIndex(index)).toInt() shl 16) or
//                (buffer.getByte(nextIndex(nextIndex(index))).toInt() shl 8) or
//                buffer.getByte(nextIndex(nextIndex(nextIndex(index)))).toInt())

        return ((buffer.getByte(index).toUByte().toInt() shl 24) or
                (buffer.getByte(nextIndex(index)).toUByte().toInt() shl 16) or
                (buffer.getByte(nextIndex(nextIndex(index))).toUByte().toInt() shl 8) or
                buffer.getByte(nextIndex(nextIndex(nextIndex(index)))).toUByte().toInt())
    }

    fun peekInt16(index: Int): Short {
        if (isReadIndexValid(index, Short.SIZE_BYTES)) {
            return getInt16(index)
        } else {
            throw IndexOutOfBoundsException("Cannot peek at the specified offset")
        }
    }

    fun peekInt32(index: Int): Int {
        if (isReadIndexValid(index, Int.SIZE_BYTES)) {
            return getInt32(index)
        } else {
            throw IndexOutOfBoundsException("Cannot peek at the specified offset")
        }
    }

    fun readInt16(): Short {
        val data = peekInt16(readIndex)
        val count = Short.SIZE_BYTES
        readIndex = moveIndex(readIndex, count)
        size -= count
        return data
    }

    fun readInt32(): Int {
        val data = peekInt32(readIndex)
        val count = Int.SIZE_BYTES
        readIndex = moveIndex(readIndex, count)
        size -= count
        return data
    }

    fun setInt16(index: Int, value: Short) {
        if(!isReadIndexValid(index,Short.SIZE_BYTES)) {
            throw IndexOutOfBoundsException("Cannot replace value outside of the ring buffer")
        }
        buffer.setByte(index, (value.toInt() ushr 8))
        buffer.setByte(nextIndex(index), (value.toInt() and 0xFF))
    }

    fun writeInt16(value: Short): Int {
        val count = Short.SIZE_BYTES
        if (size + count > capacity) {
            resizeBuffer(buffer.capacity() * 2)
        }
        val startIndex = writeIndex
        enqueue((value.toInt() ushr 8).toByte())
        enqueue((value.toInt() and 0xFF).toByte())
        return startIndex
    }

    fun writeInt32(value: Int): Int {
        val count = Int.SIZE_BYTES
        if (size + count > capacity) {
            resizeBuffer(buffer.capacity() * 2)
        }
        val startIndex = writeIndex

        enqueue(((value shr 24) and 0xFF).toByte()) // 상위 바이트 (1번째 바이트)
        enqueue(((value shr 16) and 0xFF).toByte()) // 2번째 바이트
        enqueue(((value shr 8) and 0xFF).toByte())  // 3번째 바이트
        enqueue((value and 0xFF).toByte())

        return startIndex
    }

    fun toArray(): ByteArray {
        val data = ByteArray(size)
        for (i in 0 until size) {
            data[i] = dequeue()
        }
        return data
    }
}