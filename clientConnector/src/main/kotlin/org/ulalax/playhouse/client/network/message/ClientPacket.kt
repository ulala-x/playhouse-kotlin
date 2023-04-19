package org.ulalax.playhouse.client.network.message
import com.google.protobuf.ByteString
import com.google.protobuf.GeneratedMessageV3
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufOutputStream
import org.ulalax.playhouse.THBuffer
import org.ulalax.playhouse.client.network.ByteBufferAllocator
import org.ulalax.playhouse.client.network.PacketParser
import java.io.IOException
import java.nio.ByteBuffer


data class TargetId(val serviceId:Short,val stageIndex:Int = 0){
    init {
        if(stageIndex > Byte.MAX_VALUE){
            throw ArithmeticException("stageIndex overflow")
        }
    }
}


class Header constructor(var serviceId: Short= 0,
                         var msgId:Int= 0,
                         var msgSeq:Short=0,
                         var errorCode:Short = 0 ,
                         var stageIndex:Byte = 0)

interface Payload : AutoCloseable {
    fun data():ByteBuffer
}

class EmptyPayload :Payload {

    private  var buffer:ByteBuf
    init {
        buffer= ByteBufferAllocator.getBuf(0)
    }
    override fun data(): ByteBuffer {
        return buffer.nioBuffer()
    }
    override fun close() {
        buffer.release()
    }
}



class BytePayload : Payload {

    private  var buffer:ByteBuf
    constructor(buffer:ByteBuf){
        this.buffer = buffer
    }
    override fun data(): ByteBuffer{
        return this.buffer.nioBuffer()
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

class ProtoPayload(private val proto: GeneratedMessageV3) : Payload {

    private var byteBuffer:ByteBuffer? = null
    override fun data(): ByteBuffer {
        if(byteBuffer==null){
            val buffer = THBuffer.buffer()
            val outputStream = ByteBufOutputStream(buffer)
            proto.writeTo(outputStream)
            byteBuffer = buffer.nioBuffer()
        }
        return byteBuffer!!
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

class ClientPacket private constructor(val header: Header, private var payload: Payload) : BasePacket {
    companion object{
        fun toServerOf(targetId: TargetId, packet: Packet): ClientPacket {
            val header = Header(msgId = packet.msgId,serviceId = targetId.serviceId,stageIndex = targetId.stageIndex.toByte())

            return  ClientPacket(header,packet.payload)
        }

        fun of(header: Header, payload: Payload): ClientPacket {
            return ClientPacket(header,payload)
        }

    }

    fun movePayload(): Payload {
        val temp = payload
        payload = EmptyPayload()
        return temp;
    }

    override fun data(): ByteBuffer {
        return payload.data()
    }


    fun serviceId():Short {
        return header.serviceId
    }
    fun msgId():Int {
        return header.msgId
    }

    fun toReplyPacket(): ReplyPacket {
        return ReplyPacket(header.errorCode,header.msgId,movePayload())
    }

    fun header(): Header {
        return header;
    }

    fun toByteBuf(buffer:ByteBuf) {
        val body = payload.data()
        val bodySize = body.limit()

        if(bodySize > PacketParser.MAX_PACKET_SIZE){
            throw IOException("body size is over : $bodySize");
        }

        val packetSize = PacketParser.HEADER_SIZE-2 + bodySize

        buffer.capacity(packetSize)
        buffer.writeShort(bodySize)
        buffer.writeShort(header.serviceId.toInt())
        buffer.writeInt(header.msgId)
        buffer.writeShort(header.msgSeq.toInt())
        buffer.writeByte(header.stageIndex.toInt())
        buffer.writeBytes(payload.data())
    }

    fun setMsgSeq(msgSeq: Short) {
        this.header.msgSeq = msgSeq
    }

    fun toPacket(): Packet {
        return Packet(header.msgId,movePayload())
    }

    override fun close() {
        payload.close()
    }



}

