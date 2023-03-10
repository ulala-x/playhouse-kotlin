package org.ulalax.playhouse.protocol
import com.google.protobuf.ByteString
import com.google.protobuf.GeneratedMessageV3
import io.netty.buffer.ByteBuf
import io.netty.buffer.UnpooledByteBufAllocator
import org.ulalax.playhouse.protocol.Common.*
import org.apache.logging.log4j.kotlin.logger
import org.ulalax.playhouse.base.PacketParser
import org.zeromq.ZFrame

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
    fun frame():ZFrame
}

class ProtoPayload constructor() : Payload {
    private val log = logger()
    constructor(message: GeneratedMessageV3) : this() {
        this.proto = message
    }
    constructor(message: ByteString): this(){
        this.byteString = message
    }

    constructor(message: ZFrame): this(){
        this.frame = message
    }

    private var frame: ZFrame? = null
    private var byteString:ByteString? = null
    private var proto:GeneratedMessageV3? = null
    override fun close() {
        frame?.apply {
            this.close()
        }

        frame = null
        byteString = null
        proto = null
    }

    override fun frame(): ZFrame {
        if(frame == null){
            proto?.apply {
                frame = ZFrame(proto!!.toByteArray());
            }
            byteString?.apply {
                frame = ZFrame(byteString!!.toByteArray());
            }
        }

        return frame ?: ZFrame(ByteArray(0))
    }

    fun proto(): GeneratedMessageV3? {
        return proto
    }

}

class ClientPacket private constructor(val header: Header, private var payload: Payload) : BasePacket,ReqPacket {
    companion object{
        fun toServerOf(serviceId: String, packet: Packet): ClientPacket {
            val header = Header(msgName = packet.msgName,serviceId = serviceId)

            return  ClientPacket(header,packet.movePayload())
        }

        fun of(headerMsg: HeaderMsg, payload: Payload): ClientPacket {
            return ClientPacket(Header.of(headerMsg),payload)
        }

//        fun ofErrorPacket(errorCode: Int): ReplyPacket {
//            return ReplyPacket(errorCode,);
//        }

        private val buffer = UnpooledByteBufAllocator.DEFAULT.buffer(PacketParser.MAX_PACKET_SIZE)
    }

    override fun movePayload(): Payload {
        val temp = payload
        payload = ProtoPayload()
        return temp;
    }

    override fun frame():ZFrame {
        return this.payload.frame()
    }

    override fun data(): ByteArray {
        return frame().data();
    }

    fun serviceId():String {
        return header.serviceId
    }
    fun msgName():String {
        return header.msgName
    }

    override  fun toReplyPacket(): ReplyPacket {
        return ReplyPacket(header.errorCode,header.msgName,movePayload())
    }

    override fun header(): Header {
        return header;
    }

    fun toByteBuf(buffer:ByteBuf) {

        val header = header.toMsg()
        val headerSize = header.serializedSize
        val body = frame()
        val packetSize =1+2+headerSize+body.length()

        buffer.capacity(packetSize)

//        val buffer = ByteBufferAllocator.getBuf(packetSize)

        /*
        1byte - header size
        2byte - body size
        header
        body
         */
        buffer.writeByte(headerSize)
        buffer.writeShort(body.length())
        buffer.writeBytes(header.toByteArray())
        buffer.writeBytes(body.data())

        //return buffer
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

