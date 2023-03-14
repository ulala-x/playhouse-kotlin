package org.ulalax.playhouse.client.network.message

import com.google.protobuf.ByteString
import com.google.protobuf.GeneratedMessageV3
import io.netty.buffer.ByteBuf

interface   ReplyCallback{
    fun onReceive(replyPacket: ReplyPacket)
}

interface BasePacket : AutoCloseable {
    fun movePayload(): Payload
    fun data():ByteArray
}


data class Packet @JvmOverloads  constructor(val msgName:String="",var payload: Payload = BytePayload()) :BasePacket {
    constructor(message: GeneratedMessageV3) : this(message.descriptorForType.name, BytePayload(message.toByteArray()))
    constructor( msgName: String, message:ByteString):this(msgName, BytePayload(message.toByteArray()))

    override fun data(): ByteArray {
        return this.payload.buffer().array();
    }

    override fun movePayload(): Payload {
        val temp = payload
        payload = BytePayload()
        return temp;
    }
    override fun close() {
        this.payload.close()
    }
}
data class ReplyPacket @JvmOverloads constructor(val errorCode: Int,val msgName:String="", private var payload: Payload = BytePayload()):
    BasePacket {

    constructor(message: GeneratedMessageV3) : this(0,message.descriptorForType.name, BytePayload(message.toByteArray()))
    constructor(errorCode: Int,message: GeneratedMessageV3) : this(errorCode,message.descriptorForType.name,
            BytePayload(message.toByteArray())
    )
    fun isSuccess():Boolean{
        return errorCode == 0
    }

    override fun data(): ByteArray {
        return payload.buffer().array()
    }

    override fun movePayload(): Payload {
        val temp = payload
        payload = BytePayload()
        return temp;
    }
    override fun close() {
        payload.close()
    }


}