package org.ulalax.playhouse.communicator.message

import com.google.protobuf.ByteString
import com.google.protobuf.GeneratedMessageV3
import org.ulalax.playhouse.protocol.Server.*
import java.nio.ByteBuffer

interface   ReplyCallback{
    fun onReceive(replyPacket: ReplyPacket)
}

interface BasePacket : AutoCloseable {
    fun movePayload(): Payload
    fun data(): ByteBuffer

}

class Header constructor(var serviceId: Short=0, var msgId:Int=0, var msgSeq:Short=0, var errorCode:Short = 0,val stageIndex:UByte = 0u ){
    companion object {
        fun of(headerMsg: HeaderMsg): Header {
            return Header(headerMsg.serviceId.toShort(),headerMsg.msgId,headerMsg.msgSeq.toShort(),headerMsg.errorCode.toShort())
        }
    }
    fun toMsg(): HeaderMsg {
        return HeaderMsg.newBuilder()
            .setServiceId(this.serviceId.toInt())
            .setMsgId(this.msgId)
            .setMsgSeq(this.msgSeq.toInt())
            .setStageIndex(this.stageIndex.toInt())
            .setErrorCode(this.errorCode.toInt()).build()
    }
}
data class Packet @JvmOverloads  constructor(val msgId:Int=-1, var payload: Payload = EmptyPayload()) : BasePacket {
    constructor(message: GeneratedMessageV3) : this(message.descriptorForType.index, ProtoPayload(message))
    constructor(msgId: Int, message:ByteString):this(msgId, ByteStringPayload(message))

    override fun data(): ByteBuffer {
        return this.payload.data()
    }

    override fun movePayload(): Payload {
        val temp = payload
        payload = EmptyPayload()
        return temp;
    }
    override fun close() {
        this.payload.close()
    }
}
data class ReplyPacket @JvmOverloads constructor(val errorCode: Short, val msgId:Int=-1, private var payload: Payload = EmptyPayload()):
    BasePacket {

    constructor(message: GeneratedMessageV3) : this(0,message.descriptorForType.index, ProtoPayload(message))
    constructor(errorCode: Short,message: GeneratedMessageV3) : this(errorCode,message.descriptorForType.index, ProtoPayload(message))
    fun isSuccess():Boolean{
        return errorCode == 0.toShort()
    }

    override fun data(): ByteBuffer {
        return payload.data()
    }

    override fun movePayload(): Payload {
        val temp = payload
        payload = EmptyPayload()
        return temp;
    }
    override fun close() {
        payload.close()
    }
}