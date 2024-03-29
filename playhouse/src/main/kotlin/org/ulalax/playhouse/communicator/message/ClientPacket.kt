package org.ulalax.playhouse.communicator.message
import java.nio.ByteBuffer


class ClientPacket private constructor(val header: Header, var payload: Payload) : BasePacket {
    companion object{
        fun toServerOf(serviceId: Short, packet: Packet): ClientPacket {
            val header = Header(msgId = packet.msgId,serviceId = serviceId)
            return  ClientPacket(header,packet.movePayload())
        }
        fun of(header: Header, payload: Payload): ClientPacket {
            return ClientPacket(header,payload)
        }
    }

    override fun movePayload(): Payload {
        val temp = payload
        payload = EmptyPayload()
        return temp;
    }

    override fun data(): ByteBuffer {
        return payload.data()
    }


    val serviceId:Short
        get() = header.serviceId
    val msgId:Int
        get() =  header.msgId


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

