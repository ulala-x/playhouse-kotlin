package org.ulalax.playhouse.communicator.message

import com.google.protobuf.ByteString
import com.google.protobuf.GeneratedMessageV3
import org.zeromq.ZFrame
import java.io.OutputStream


interface Payload : AutoCloseable{
    fun data():ByteArray
    fun output(outputStream: OutputStream)
}
class ProtoPayload(val proto: GeneratedMessageV3) : Payload {

    override fun data(): ByteArray {
        return proto.toByteArray()
    }

    override fun output(outputStream: OutputStream) {
        proto.writeTo(outputStream)
    }

    override fun close() {
    }
}
class ByteStringPayload(private val byteString: ByteString): Payload{
    override fun data(): ByteArray {
        return byteString.toByteArray()
    }

    override fun output(outputStream: OutputStream) {
        byteString.writeTo(outputStream)
    }

    override fun close() {
    }
}
class EmptyPayload :Payload {

    override fun data(): ByteArray {
        return ByteArray(0)
    }

    override fun output(outputStream: OutputStream) {
    }


    override fun close() {
    }
}

class ByteArrayPayload(var byteArray: ByteArray):Payload{
    override fun data(): ByteArray {
        return byteArray
    }

    override fun output(outputStream: OutputStream) {
        outputStream.write(byteArray)
    }

    override fun close() {
    }
}


class FramePayload(var frame: ZFrame) : Payload{
    override fun data(): ByteArray {
        return frame.data()
    }

    override fun output(outputStream: OutputStream) {
        outputStream.write(frame.data())
    }

    override fun close(){
        frame.close()
    }
}
