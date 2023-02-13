package org.ulalax.playhouse.protocol

import com.google.protobuf.ByteString
import com.google.protobuf.GeneratedMessageV3
import org.zeromq.ZFrame
import java.nio.ByteBuffer

interface   ReplyCallback{
    fun onReceive(replyPacket: ReplyPacket)
    fun throws(exception: Exception)
}

interface BasePacket : AutoCloseable {
    fun movePayload(): Payload
    fun frame():ZFrame
    fun data():ByteArray

}

data class Packet @JvmOverloads  constructor(val msgName:String="",var payload: Payload = ProtoPayload()) : BasePacket {
    constructor(message: GeneratedMessageV3) : this(message.descriptorForType.name, ProtoPayload(message))
    constructor( msgName: String, message:ByteString):this(msgName, ProtoPayload(message))

    override fun frame(): ZFrame {
        return payload.frame()
    }

    override fun data(): ByteArray {
        return this.frame().data()
    }

    override fun movePayload(): Payload {
        val temp = payload
        payload = ProtoPayload()
        return temp;
    }
    override fun close() {
        this.payload.close()
    }
}
data class ReplyPacket @JvmOverloads constructor(val errorCode: Int,val msgName:String="", private var payload: Payload = ProtoPayload()):
    BasePacket {

    constructor(message: GeneratedMessageV3) : this(0,message.descriptorForType.name, ProtoPayload(message))
    constructor(errorCode: Int,message: GeneratedMessageV3) : this(errorCode,message.descriptorForType.name,
        ProtoPayload(message)
    )
    fun isSuccess():Boolean{
        return errorCode == 0
    }
    override fun frame(): ZFrame {
        return payload.frame()
    }

    override fun data(): ByteArray {
        return frame().data()
    }

    override fun movePayload(): Payload {
        val temp = payload
        payload = ProtoPayload()
        return temp;
    }
    override fun close() {
        payload.close()
    }


}