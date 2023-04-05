package org.ulalax.playhouse

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RingBufferTest : FunSpec({


    test("enqueue(item: Byte) should add an item to the buffer") {
        val ringBuffer = RingBuffer(4)
        ringBuffer.enqueue(1)
        ringBuffer.count shouldBe 1
        ringBuffer.peek() shouldBe 1
    }

    test("enqueue(item: Byte) should resize the buffer when its capacity is reached") {
        val ringBuffer = RingBuffer(4,8)
        ringBuffer.enqueue(1)
        ringBuffer.enqueue(2)
        ringBuffer.enqueue(3)
        ringBuffer.enqueue(4)
        ringBuffer.count shouldBe 4
        ringBuffer.capacity shouldBe 4
        ringBuffer.enqueue(5)
        ringBuffer.count shouldBe 5
        ringBuffer.capacity shouldBe 8
        ringBuffer.peek() shouldBe 1
    }

    test("enqueue(item: Byte) should throw an exception when the buffer has reached maximum capacity") {
        val ringBuffer = RingBuffer(4,8)
        ringBuffer.enqueue(1)
        ringBuffer.enqueue(2)
        ringBuffer.enqueue(3)
        ringBuffer.enqueue(4)
        ringBuffer.enqueue(5)
        ringBuffer.enqueue(6)
        ringBuffer.enqueue(7)
        ringBuffer.enqueue(8)
        ringBuffer.count shouldBe 8
        ringBuffer.capacity shouldBe 8
        shouldThrow<IllegalStateException> {
            ringBuffer.enqueue(9)
        }
    }

    test("enqueue(data: ByteArray) should add an array of data to the buffer") {
        val ringBuffer = RingBuffer(4,8)
        val data = byteArrayOf(1, 2, 3, 4)
        ringBuffer.enqueue(data)
        ringBuffer.count shouldBe 4
        ringBuffer.peek() shouldBe 1
    }

    test("resizeBuffer(newCapacity: Int) should resize the buffer and preserve its contents") {
        val ringBuffer = RingBuffer(4,8)
        ringBuffer.enqueue(1)
        ringBuffer.enqueue(2)
        ringBuffer.enqueue(3)
        ringBuffer.enqueue(4)
        ringBuffer.count shouldBe 4
        ringBuffer.capacity shouldBe 4

        ringBuffer.enqueue(5)
        ringBuffer.capacity shouldBe 8
        ringBuffer.count shouldBe 5

        ringBuffer.peek() shouldBe 1
    }

    test("dequeue() should remove an item from the buffer") {
        val ringBuffer = RingBuffer(4,8)
        ringBuffer.enqueue(1)
        ringBuffer.enqueue(2)
        ringBuffer.enqueue(3)
        ringBuffer.count shouldBe 3
        ringBuffer.dequeue() shouldBe 1
        ringBuffer.count shouldBe 2
        ringBuffer.peek() shouldBe 2
    }

    test("dequeue() should throw an exception when the buffer is empty") {
        val ringBuffer = RingBuffer(4)
        shouldThrow<IllegalStateException> {
            ringBuffer.dequeue()
        }
    }

    test("peek() should return the first item in the buffer") {
        val ringBuffer = RingBuffer(4)
        ringBuffer.enqueue(1)
        ringBuffer.enqueue(2)
        ringBuffer.enqueue(3)
        ringBuffer.peek() shouldBe 1
    }

    test("peek() should throw an exception when the buffer is empty") {
        val ringBuffer = RingBuffer(4)
        shouldThrow<IllegalStateException> {
            ringBuffer.peek()
        }
    }

    test("clear() should clear the buffer") {
        val ringBuffer = RingBuffer(4)
        ringBuffer.enqueue(1)
        ringBuffer.enqueue(2)
        ringBuffer.enqueue(3)
        ringBuffer.count shouldBe 3
        ringBuffer.clear()
        ringBuffer.count shouldBe 0
    }

    test("clear(count: Int) should remove the specified number of items from the buffer") {
        val ringBuffer = RingBuffer(4)
        ringBuffer.enqueue(1)
        ringBuffer.enqueue(2)
        ringBuffer.enqueue(3)
        ringBuffer.count shouldBe 3
        ringBuffer.clear(2)
        ringBuffer.count shouldBe 1
        ringBuffer.peek() shouldBe 3
    }

    test("clear(count: Int) should throw an exception when trying to clear more items than the current size of the buffer") {
        val ringBuffer = RingBuffer(4)
        ringBuffer.enqueue(1)
        ringBuffer.enqueue(2)
        ringBuffer.enqueue(3)
        ringBuffer.count shouldBe 3
        shouldThrow<IllegalArgumentException> {
            ringBuffer.clear(4)
        }
    }
    test("read(buffer: ByteArray, offset: Int, count: Int) should read items from the buffer") {
        val ringBuffer = RingBuffer(4)
        ringBuffer.enqueue(1)
        ringBuffer.enqueue(2)
        ringBuffer.enqueue(3)
        ringBuffer.count shouldBe 3
        val buffer = ByteArray(2)
        ringBuffer.read(buffer, 0, 2)
        buffer shouldBe byteArrayOf(1, 2)
        ringBuffer.count shouldBe 1
        ringBuffer.peek() shouldBe 3
    }

    test("read(buffer: ByteArray, offset: Int, count: Int) should be read size is 0 when the buffer is empty") {
        val ringBuffer = RingBuffer(4)
        val buffer = ByteArray(2)

        ringBuffer.read(buffer, 0, 2) shouldBe 0

    }

    test("write(buffer: ByteArray, offset: Int, count: Int) should write items to the buffer") {
        val ringBuffer = RingBuffer(4)
        val data = byteArrayOf(1, 2, 3, 4)
        ringBuffer.write(data, 0, 4)
        ringBuffer.count shouldBe 4
        ringBuffer.peek() shouldBe 1
    }

    test("write(buffer: ByteArray, offset: Int, count: Int) should resize the buffer when its capacity is reached") {
        val ringBuffer = RingBuffer(4,16)
        val data = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        ringBuffer.write(data, 0, 8)
        ringBuffer.count shouldBe 8
        ringBuffer.capacity shouldBe 8
        ringBuffer.write(data, 0, 1)
        ringBuffer.count shouldBe 9
        ringBuffer.capacity shouldBe 16
        ringBuffer.peek() shouldBe 1
    }

    test("peekInt16(index: Int) should return the correct Short value when peeking at the specified offset") {
        val ringBuffer = RingBuffer(4)
        ringBuffer.writeInt16(0x1234.toShort())
        ringBuffer.writeInt16(0x5678.toShort())
        ringBuffer.peekInt16(2) shouldBe 0x5678.toShort()
    }

    test("peekInt16(index: Int) should throw an exception when trying to peek outside of the buffer") {
        val ringBuffer = RingBuffer(4)
        shouldThrow<IndexOutOfBoundsException> {
            ringBuffer.peekInt16(2)
        }
    }

    test("peekInt32(index: Int) should return the correct Int value when peeking at the specified offset") {
        val ringBuffer = RingBuffer(8)
        ringBuffer.writeInt32(0x12345678)
        ringBuffer.writeInt32(0x5678DEF0)
        ringBuffer.peekInt32(4) shouldBe 0x5678DEF0
    }

    test("peekInt32(offset: Int) should throw an exception when trying to peek outside of the buffer") {
        val ringBuffer = RingBuffer(4)
        shouldThrow<IndexOutOfBoundsException> {
            ringBuffer.peekInt32(4)
        }
    }

    test("readInt16() should return the correct Short value and update the buffer") {
        val ringBuffer = RingBuffer(4)
        ringBuffer.writeInt16(0x1234.toShort())
        ringBuffer.writeInt16(0x5678.toShort())
        ringBuffer.readInt16() shouldBe 0x1234.toShort()
        ringBuffer.count shouldBe 2
        ringBuffer.peek() shouldBe 0x56.toByte()
    }

    test("readInt16() should throw an exception when trying to read outside of the buffer") {
        val ringBuffer = RingBuffer(4)
        shouldThrow<IndexOutOfBoundsException> {
            ringBuffer.readInt16()
        }
    }
    test("readInt32() should return the correct Int value and update the buffer") {
        val ringBuffer = RingBuffer(8)
        ringBuffer.writeInt32(0x12345678)
        ringBuffer.writeInt32(0x5678DEF0)
        ringBuffer.readInt32() shouldBe 0x12345678
        ringBuffer.count shouldBe 4
        ringBuffer.peek() shouldBe 0x56.toByte()
    }

    test("readInt32() should throw an exception when trying to read outside of the buffer") {
        val ringBuffer = RingBuffer(4)
        shouldThrow<IndexOutOfBoundsException> {
            ringBuffer.readInt32()
        }
    }

    test("setInt16(index: Int, value: Short) should set the Short value at the specified index") {
        val ringBuffer = RingBuffer(4)
        ringBuffer.writeInt16(0x1234.toShort())
        ringBuffer.writeInt16(0x5678.toShort())
        ringBuffer.setInt16(2, 0x9ABC.toShort())
        ringBuffer.peekInt16(2) shouldBe 0x9ABC.toShort()
    }

    test("setInt16(index: Int, value: Short) should throw an exception when trying to set a value outside of the buffer") {
        val ringBuffer = RingBuffer(4)
        shouldThrow<IndexOutOfBoundsException> {
            ringBuffer.setInt16(2, 0x1234.toShort())
        }
    }

    test("writeInt16(value: Short) should write the Short value to the buffer and return the start index") {
        val ringBuffer = RingBuffer(8)
        ringBuffer.writeInt16(0x1234.toShort())
        ringBuffer.writeInt16(0x5678.toShort())
        ringBuffer.writeInt16(0x9ABC.toShort()) shouldBe 4
        ringBuffer.count shouldBe 6
        ringBuffer.peek() shouldBe 0x12.toByte()
    }

    test("writeInt16(value: Short) should resize the buffer when its capacity is reached") {
        val ringBuffer = RingBuffer(4,8)
        ringBuffer.writeInt16(0x1234.toShort())
        ringBuffer.writeInt16(0x5678.toShort())
        ringBuffer.writeInt16(0x9ABC.toShort()) shouldBe 4
        ringBuffer.count shouldBe 6
        ringBuffer.capacity shouldBe 8
        ringBuffer.writeInt16(0xDEF0.toShort()) shouldBe 6
        ringBuffer.count shouldBe 8
        ringBuffer.capacity shouldBe 8
    }

    test("writeInt32(value: Int) should write the Int value to the buffer and return the start index") {
        val ringBuffer = RingBuffer(16)
        ringBuffer.writeInt32(0x12345678)
        ringBuffer.writeInt32(0x5678DEF0)
        ringBuffer.writeInt32(0x11223344) shouldBe 8
        ringBuffer.count shouldBe 12
        ringBuffer.peek() shouldBe 0x12.toByte()
    }

    test("writeInt32(value: Int) should resize the buffer when its capacity is reached") {
        val ringBuffer = RingBuffer(8,16)
        ringBuffer.writeInt32(0x12345678)
        ringBuffer.writeInt32(0x5678DEF0)
        ringBuffer.writeInt32(0x11223344) shouldBe 8
        ringBuffer.count shouldBe 12
        ringBuffer.capacity shouldBe 16
        ringBuffer.writeInt32(0x55667788) shouldBe 12
        ringBuffer.count shouldBe 16
        ringBuffer.capacity shouldBe 16
    }

    test("toArray() should return an array with the contents of the buffer") {
        val ringBuffer = RingBuffer(4)
        ringBuffer.writeInt16(0x1234.toShort())
        ringBuffer.writeInt16(0x5678.toShort())
        ringBuffer.toArray() shouldBe byteArrayOf(0x12, 0x34, 0x56, 0x78)
    }




})