package org.ulalax.playhouse.base.protocol

import com.google.protobuf.ByteString
import com.google.protobuf.GeneratedMessageV3
import io.netty.buffer.ByteBuf
import java.nio.ByteBuffer

interface   ReplyCallback{
    fun onReceive(replyPacket: ReplyPacket)
    fun throws(exception: Exception)
}

interface BasePacket : AutoCloseable {
    fun movePayload():Payload
    fun buffer():ByteBuffer

}

data class Packet @JvmOverloads  constructor(val msgName:String="",var payload: Payload = ProtoPayload()) : BasePacket {
    constructor(message: GeneratedMessageV3) : this(message.descriptorForType.name,ProtoPayload(message))
    constructor( msgName: String, message:ByteString):this(msgName,ProtoPayload(message))

    override fun buffer(): ByteBuffer {
        return payload.buffer().nioBuffer()
    }
    override fun movePayload():Payload{
        val temp = payload
        payload = ProtoPayload()
        return temp;
    }
    override fun close() {
        this.payload.close()
    }
}
data class ReplyPacket @JvmOverloads constructor(val errorCode: Int,val msgName:String="", private var payload: Payload = ProtoPayload()): BasePacket {

    constructor(message: GeneratedMessageV3) : this(0,message.descriptorForType.name, ProtoPayload(message))
    constructor(errorCode: Int,message: GeneratedMessageV3) : this(errorCode,message.descriptorForType.name,ProtoPayload(message))
    fun isSuccess():Boolean{
        return errorCode == 0
    }
    override fun buffer(): ByteBuffer {
        return payload.buffer().nioBuffer()
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