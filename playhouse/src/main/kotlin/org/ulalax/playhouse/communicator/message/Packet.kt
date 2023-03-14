package org.ulalax.playhouse.communicator.message

import com.google.protobuf.ByteString
import com.google.protobuf.GeneratedMessageV3
import org.ulalax.playhouse.protocol.Common
import org.zeromq.ZFrame

interface   ReplyCallback{
    fun onReceive(replyPacket: ReplyPacket)
}

interface BasePacket : AutoCloseable {
    fun movePayload(): Payload
    fun frame():ZFrame
    fun data():ByteArray

}

class Header constructor(var msgName:String="",var errorCode:Int = 0,var msgSeq:Int=0, var serviceId: String=""){
    companion object {
        fun of(headerMsg: Common.HeaderMsg): Header {
            return Header(headerMsg.msgName,headerMsg.errorCode,headerMsg.msgSeq,headerMsg.serviceId)
        }
    }
    fun toMsg(): Common.HeaderMsg {
        return Common.HeaderMsg.newBuilder()
                .setServiceId(this.serviceId)
                .setMsgSeq(this.msgSeq)
                .setMsgName(this.msgName)
                .setErrorCode(this.errorCode).build()
    }
}
data class Packet @JvmOverloads  constructor(val msgName:String="",var payload: Payload = XPayload()) : BasePacket {
    constructor(message: GeneratedMessageV3) : this(message.descriptorForType.name, XPayload(message))
    constructor( msgName: String, message:ByteString):this(msgName, XPayload(message))

    override fun frame(): ZFrame {
        return payload.frame()
    }

    override fun data(): ByteArray {
        return this.frame().data()
    }

    override fun movePayload(): Payload {
        val temp = payload
        payload = XPayload()
        return temp;
    }
    override fun close() {
        this.payload.close()
    }
}
data class ReplyPacket @JvmOverloads constructor(val errorCode: Int,val msgName:String="", private var payload: Payload = XPayload()):
    BasePacket {

    constructor(message: GeneratedMessageV3) : this(0,message.descriptorForType.name, XPayload(message))
    constructor(errorCode: Int,message: GeneratedMessageV3) : this(errorCode,message.descriptorForType.name,
        XPayload(message)
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
        payload = XPayload()
        return temp;
    }
    override fun close() {
        payload.close()
    }
}