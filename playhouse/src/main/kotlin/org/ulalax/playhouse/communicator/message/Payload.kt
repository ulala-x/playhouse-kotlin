package org.ulalax.playhouse.communicator.message

import com.google.protobuf.ByteString
import com.google.protobuf.GeneratedMessageV3
import io.netty.buffer.ByteBufOutputStream
import org.ulalax.playhouse.THBuffer
import org.zeromq.ZFrame
import java.io.OutputStream
import java.nio.ByteBuffer


interface Payload : AutoCloseable{
    fun data():ByteBuffer
}
class ProtoPayload(val proto: GeneratedMessageV3) : Payload {

    override fun data(): ByteBuffer {
        val buffer = THBuffer.buffer()
        val outputStream = ByteBufOutputStream(buffer)
        this.proto.writeTo(outputStream)
        return buffer.nioBuffer()
    }

    override fun close() {
    }
}
class ByteStringPayload(private val byteString: ByteString): Payload{
    override fun data(): ByteBuffer {
        val buffer = THBuffer.buffer()
        val outputStream = ByteBufOutputStream(buffer)
        this.byteString.writeTo(outputStream)
        return buffer.nioBuffer()
    }

    override fun close() {
    }
}
class EmptyPayload :Payload {
    override fun data(): ByteBuffer {
        return ByteBuffer.allocate(0)
    }
    override fun close() {
    }
}


class FramePayload(var frame: ZFrame) : Payload{
    override fun data(): ByteBuffer {
        return ByteBuffer.wrap(frame.data())
    }
    override fun close(){
        frame.close()
    }
}
