package org.ulalax.playhouse.communicator.message

import java.io.OutputStream

class PreAllocByteArrayOutputStream(private val buffer: ByteArray) : OutputStream() {
    private var position: Int = 0

    @Throws(IndexOutOfBoundsException::class)
    override fun write(value: Int) {
        if (position >= buffer.size) {
            throw IndexOutOfBoundsException("Buffer is full")
        }
        buffer[position++] = value.toByte()
    }

    fun writtenDataLength(): Int {
        return position
    }

    fun reset() {
        position = 0
    }

    fun writeByte(value: Int) {
        write(value)
    }

    fun writeShort(value: Int) : Int {
        val startIndex = position
        replaceShort(position,value)
        position += 2
        return startIndex
    }

    fun replaceShort(index:Int,value:Int) {
        var tempIndex = index
        buffer[tempIndex++] = (value ushr 8).toByte()
        buffer[tempIndex++] = (value and 0xFF).toByte()
    }

    fun getShort(index:Int): Int {
//        val byteArray = buffer.sliceArray(index until index+2)
//        require(byteArray.size == 2) { "ByteArray size should be 4 for Int conversion" }

        if (index + 1 >= buffer.size)
        {
            throw  IndexOutOfBoundsException("Index is out of bounds");
        }
//        return  (buffer[index].toInt() and 0xFF shl 8) or (buffer[index+1].toInt() and 0xFF)
        return  (buffer[index].toInt() shl 8) or (buffer[index+1].toInt() and 0xFF)
    }

    fun array(): ByteArray {
        return buffer
    }

}
