package org.ulalax.playhouse

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.ByteOrder

class XBitConverterTest : FunSpec({

    val IsLittleEndian = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN
    beforeSpec {
        println("System Endian: ${ByteOrder.nativeOrder()}")
    }

    test("short to network order") {
        val input = 100.toShort()
        val expected = if (IsLittleEndian) {
            25600.toShort()
        } else {
            100.toShort()
        }
        XBitConverter.toNetworkOrder(input) shouldBe expected
    }

    test("short to byte array") {
        val input = 100.toShort()
        val expected = if (IsLittleEndian) {
            byteArrayOf(0x00, 0x64)
        } else {
            byteArrayOf(0x64, 0x00)
        }
        val actual = ByteArray(2)
        XBitConverter.toByteArray(input, actual, 0, actual.size)
        actual shouldBe expected
    }

    test("int to network order") {
        val input = 1000000
        val expected = if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            0x00.toByte().toInt() or
                    0x0F.shl(8) or
                    0x42.shl(16) or
                    0x40.shl(24)
        } else {
            1000000
        }
        XBitConverter.toNetworkOrder(input) shouldBe expected

    }

    test("int to byte array") {
        val input = 1000000
        val expected = if (IsLittleEndian) {
            byteArrayOf(0x00, 0x0F, 0x42, 0x40)
        } else {
            byteArrayOf(0x40, 0x42, 0x0F, 0x00)
        }
        val actual = ByteArray(4)
        XBitConverter.intToByteArray(input, actual, 0)
        actual shouldBe expected
    }


    test("byte array to short") {
        val input = if (IsLittleEndian) {
            byteArrayOf(0x00, 0x64)
        } else {
            byteArrayOf(0x64, 0x00)
        }
        val expected = 100.toShort()
        XBitConverter.byteArrayToShort(input, 0, input.size) shouldBe expected
    }

    test("byte array to int") {
        val input = if (IsLittleEndian) {
            byteArrayOf(0x00, 0x0F, 0x42, 0x40)
        } else {
            byteArrayOf(0x40, 0x42, 0x0F, 0x00)
        }
        val expected = 1000000
        XBitConverter.byteArrayToInt(input, 0, input.size) shouldBe expected
    }

    test("toHostOrder(short) should convert a big-endian value to host byte order") {
        // Arrange
        val networkOrderValue: Short = 0x1234.toShort()
        val expectedHostOrderValue: Short = if (IsLittleEndian) 0x3412.toShort() else 0x1234.toShort()

        // Act
        val actualHostOrderValue = XBitConverter.toHostOrder(networkOrderValue)

        // Assert
        actualHostOrderValue shouldBe expectedHostOrderValue
    }

    test("toHostOrder(int) should convert a big-endian value to host byte order") {
        // Arrange
        val networkOrderValue: Int = 0x12345678
        val expectedHostOrderValue: Int = if (IsLittleEndian) 0x78563412 else 0x12345678

        // Act
        val actualHostOrderValue = XBitConverter.toHostOrder(networkOrderValue)

        // Assert
        actualHostOrderValue shouldBe expectedHostOrderValue
    }
})