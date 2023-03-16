package org.ulalax.playhouse.client.network.message
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.ulalax.playhouse.protocol.Common.*

class Header constructor(var msgName:String="",var errorCode:Int = 0,var msgSeq:Int=0, var serviceId: String=""){
    companion object {
        fun of(headerMsg: HeaderMsg): Header {
            return Header(headerMsg.msgName,headerMsg.errorCode,headerMsg.msgSeq,headerMsg.serviceId)
        }
    }
    fun toMsg(): HeaderMsg {
        return HeaderMsg.newBuilder()
            .setServiceId(this.serviceId)
            .setMsgSeq(this.msgSeq)
            .setMsgName(this.msgName)
            .setErrorCode(this.errorCode).build()
    }
}

interface Payload : AutoCloseable{
    fun buffer():ByteBuf
}


class BytePayload : Payload {

    private  var buffer:ByteBuf

    constructor(){
        buffer = Unpooled.buffer()
    }
    constructor(byteArray: ByteArray){
        buffer = Unpooled.wrappedBuffer(byteArray)
    }

    override fun buffer(): ByteBuf {
        return buffer
    }

    override fun close() {
        buffer.release()
    }
}

//class ByteBufPayload():Payload{
//
//    private lateinit var buf: ByteBuf
//    override fun buffer(): ByteArray {
//        buf.to
//    }
//
//    override fun close() {
//        TODO("Not yet implemented")
//    }
//
//}
public

//class ProtoPayload constructor() : Payload {
//    private val log = logger()
//    constructor(message: GeneratedMessageV3) : this() {
//        this.proto = message
//    }
//
//    private lateinit var proto:GeneratedMessageV3;
//    override fun close() {
//
//    }
//
//    override fun buffer(): ByteBuf {
//        return Unpooled.wrappedBuffer(proto.toByteArray())
//    }
//
//    fun proto(): GeneratedMessageV3? {
//        return proto
//    }
//
//}

class ClientPacket private constructor(val header: Header, private var payload: Payload) : BasePacket {
    companion object{
        fun toServerOf(serviceId: String, packet: Packet): ClientPacket {
            val header = Header(msgName = packet.msgName,serviceId = serviceId)

            return  ClientPacket(header,packet.movePayload())
        }

        fun of(headerMsg: HeaderMsg, payload: Payload): ClientPacket {
            return ClientPacket(Header.of(headerMsg),payload)
        }

    }

    override fun movePayload(): Payload {
        val temp = payload
        payload = BytePayload()
        return temp;
    }

    override fun data(): ByteArray {
        return payload.buffer().array()
    }


    fun serviceId():String {
        return header.serviceId
    }
    fun msgName():String {
        return header.msgName
    }

    fun toReplyPacket(): ReplyPacket {
        return ReplyPacket(header.errorCode,header.msgName,movePayload())
    }

    fun header(): Header {
        return header;
    }

    fun toByteBuf(buffer:ByteBuf) {

        val header = header.toMsg()
        val headerSize = header.serializedSize
        val body = payload.buffer()
        val packetSize =1+2+headerSize+body.readableBytes()

        buffer.capacity(packetSize)

        /*
        1byte - header size
        2byte - body size
        header
        body
         */
        buffer.writeByte(headerSize)
        buffer.writeShort(body.readableBytes())
        buffer.writeBytes(header.toByteArray())
        buffer.writeBytes(body)
    }

    fun setMsgSeq(msgSeq: Int) {
        this.header.msgSeq = msgSeq
    }

    fun toPacket(): Packet {
        return Packet(header.msgName,movePayload())
    }

    override fun close() {
        payload.close()
    }



}

