package org.ulalax.playhouse

import java.nio.ByteOrder

object XBitConverter {

    private val IsLittleEndian = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN
    fun toNetworkOrder(value: Short): Short {
        return if (IsLittleEndian) {
            ((value.toInt() and 0xFF) shl 8 or ((value.toInt() ushr 8) and 0xFF)).toShort()
        } else {
            value
        }
    }

    fun toByteArray(value: Short, buffer: ByteArray, offset: Int, size: Int) {
        if (size < 2) {
            throw IllegalArgumentException("buffer size is too short: $size")
        }

        buffer[offset] = (value.toInt() ushr 8 and 0xFF).toByte() // high byte
        buffer[offset + 1] = (value.toInt() and 0xFF).toByte() // low byte
    }

    fun toNetworkOrder(value: Int): Int {
        return if (IsLittleEndian) {
            (value shl 24 or
                    (value and 0xFF00 shl 8) or
                    (value ushr 8 and 0xFF00) or
                    (value ushr 24 and 0xFF))
        } else {
            value
        }
    }

    fun intToByteArray(value: Int, buffer: ByteArray, offset: Int) {
        if (buffer.size < offset + 4) {
            throw IllegalArgumentException("buffer size is too short: length=${buffer.size}, offset=$offset")
        }
        buffer[offset] = (value ushr 24 and 0xFF).toByte() // byte 1
        buffer[offset + 1] = (value ushr 16 and 0xFF).toByte() // byte 2
        buffer[offset + 2] = (value ushr 8 and 0xFF).toByte() // byte 3
        buffer[offset + 3] = (value and 0xFF).toByte() // low byte
    }

    fun shortToByteArray(value: Short, buffer: ByteArray, offset: Int) {
        if (buffer.size < offset + 2) {
            throw IllegalArgumentException("buffer size is too short: length=${buffer.size}, offset=$offset")
        }
        buffer[offset] = (value.toInt() ushr 8 and 0xFF).toByte() // high byte
        buffer[offset + 1] = (value.toInt() and 0xFF).toByte() // low byte
    }


    fun byteArrayToShort(buffer: ByteArray, offset: Int, size: Int): Short {
        if (size != 2) {
            throw IllegalArgumentException("Byte array must have a length of 2.")
        }

        return ((buffer[offset].toInt() shl 8) or (buffer[offset + 1].toInt() and 0xFF)).toShort()
    }
    fun byteArrayToInt(buffer: ByteArray, offset: Int, size: Int): Int {
        if (size != 4) {
            throw IllegalArgumentException("Byte array must have a length of 4.")
        }

        return (buffer[offset].toInt() shl 24) or
                (buffer[offset + 1].toInt() and 0xFF shl 16) or
                (buffer[offset + 2].toInt() and 0xFF shl 8) or
                (buffer[offset + 3].toInt() and 0xFF)
    }



    fun toHostOrder(networkOrderValue: Short): Short {
        return if (IsLittleEndian) {
            val highByte = (networkOrderValue.toInt() and 0xFF00) shr 8
            val lowByte = (networkOrderValue.toInt() and 0x00FF) shl 8
            (highByte or lowByte).toShort()
        } else {
            networkOrderValue
        }
    }

    fun toHostOrder(networkOrderValue: Int): Int {
        return if (IsLittleEndian) {
            networkOrderValue shl 24 or ((networkOrderValue and 0xFF00) shl 8) or ((networkOrderValue shr 8) and 0xFF00) or (networkOrderValue shr 24 and 0xFF)
        } else {
            networkOrderValue
        }
    }

}