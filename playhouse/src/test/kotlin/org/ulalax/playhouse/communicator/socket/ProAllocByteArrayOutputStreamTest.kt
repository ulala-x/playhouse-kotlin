package org.ulalax.playhouse.communicator.socket

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe

class PreAllocByteArrayOutputStreamTest : FunSpec({

    val buffer = ByteArray(10)
    val byteArrayOutputStream = PreAllocByteArrayOutputStream(buffer)
    val value = 1234

    beforeEach {
        byteArrayOutputStream.reset()
    }

    test("Write byte") {
        byteArrayOutputStream.writeByte(65) // 'A'의 ASCII 값
        byteArrayOutputStream.writtenDataLength() shouldBeExactly 1
        buffer.sliceArray(0 until 1) shouldBe byteArrayOf(65)
    }

    test("Write short") {

        val startIndex = byteArrayOutputStream.writeShort(value)
        byteArrayOutputStream.writtenDataLength() shouldBeExactly 2
        startIndex shouldBeExactly 0
        byteArrayOutputStream.getShort(0) shouldBe value
    }

    test("Replace short") {
        val startIndex = byteArrayOutputStream.writeShort(value)
        byteArrayOutputStream.getShort(0) shouldBe value
        byteArrayOutputStream.replaceShort(startIndex, 300)
        byteArrayOutputStream.getShort(startIndex) shouldBe 300
    }

})