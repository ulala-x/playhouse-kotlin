package org.ulalax.playhouse.communicator.message
import io.netty.buffer.ByteBuf
import org.ulalax.playhouse.protocol.Common.*
import org.zeromq.ZFrame



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
        payload = XPayload()
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


    fun header(): Header {
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

