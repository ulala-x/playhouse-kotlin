package org.ulalax.playhouse.communicator.message

import com.google.protobuf.GeneratedMessageV3
import LOG
import io.netty.buffer.ByteBuf
import org.ulalax.playhouse.XBitConverter
import org.ulalax.playhouse.communicator.CommunicatorException
import org.ulalax.playhouse.communicator.ConstOption
import org.ulalax.playhouse.communicator.ConstOption.HEADER_SIZE
import org.ulalax.playhouse.communicator.ConstOption.MAX_PACKET_SIZE
import org.ulalax.playhouse.protocol.Server.*
import org.ulalax.playhouse.service.AsyncPostCallback
import org.ulalax.playhouse.service.TimerCallback
import java.io.IOException
import java.nio.ByteBuffer
import java.time.Duration


class RouteHeader private constructor(val header: Header,
                                      var sid:Int=-1,
                                      var isSystem: Boolean = false,
                                      var isBase:Boolean = false,
                                      var isBackend:Boolean = false,
                                      var isReply:Boolean = false,
                                      var accountId:Long = 0,
                                      var stageId:Long = 0,
                                      var forClient:Boolean = false
){

    var from: String = ""
    fun toMsg(): RouteHeaderMsg {
        return RouteHeaderMsg.newBuilder()
            .setHeaderMsg(header.toMsg())
            .setSid(sid)
            .setIsSystem(isSystem)
            .setIsBase(isBase)
            .setIsBackend(isBackend)
            .setIsReply(isReply)
            .setAccountId(accountId)
            .setStageId(stageId)
            .setForClient(forClient)
            .build()
    }

    companion object {
        fun of(headerMsg: RouteHeaderMsg): RouteHeader {
            return RouteHeader(
                Header.of(headerMsg.headerMsg),
                headerMsg.sid,
                headerMsg.isSystem,headerMsg.isBase,headerMsg.isBackend,headerMsg.isReply,
                headerMsg.accountId,headerMsg.stageId,headerMsg.forClient
            )
        }

        fun of(header: HeaderMsg): RouteHeader {
            return RouteHeader(Header.of(header))
        }
        fun of(header: Header): RouteHeader {
            return RouteHeader(header)
        }
        fun timerOf(stageId: Long, msgId:Short): RouteHeader {
            return RouteHeader(Header(msgId)).apply {
                this.stageId = stageId
            }
        }
    }


    fun toByteArray(): ByteArray {
        return toMsg().toByteArray()
    }

    fun msgId(): Int {
        return this.header.msgId
    }

}

open class RoutePacket protected constructor(val routeHeader: RouteHeader, private var payload: Payload) : BasePacket {

    var timerId: Long = 0
    var timerCallback: TimerCallback = {}

    fun msgId(): Int {
        return routeHeader.header.msgId
    }

    fun serviceId(): Short {
        return routeHeader.header.serviceId
    }

    fun isBackend(): Boolean {
        return routeHeader.isBackend
    }


    override fun data(): ByteBuffer {
        return payload.data();
    }

    fun toReplyPacket(): ReplyPacket {
        return ReplyPacket(routeHeader.header.errorCode, routeHeader.msgId(), movePayload())
    }

    fun header(): Header {
        return routeHeader.header
    }

    fun toClientPacket(): ClientPacket {
//        val packetMsg = PacketMsg.newBuilder()
//            .setHeaderMsg(routeHeader.header.toMsg())
//            .setMessage(message).build()
        return ClientPacket.of(routeHeader.header, movePayload())
    }

    fun toPacket(): Packet {
        return Packet(msgId(), movePayload())
    }

    fun isBase(): Boolean {
        return routeHeader.isBase
    }

    fun accountId(): Long {
        return routeHeader.accountId
    }

    fun setMsgSeq(msgSeq: Short) {
        routeHeader.header.msgSeq = msgSeq
    }

    fun isRequest(): Boolean {
        return routeHeader.header.msgSeq != 0.toShort()
    }

    fun isReply(): Boolean {
        return routeHeader.isReply
    }

    fun stageId(): Long {
        return routeHeader.stageId
    }

    fun isSystem(): Boolean {
        return routeHeader.isSystem
    }

    fun forClient(): Boolean {
        return routeHeader.forClient
    }


    companion object {
        fun moveOf(routePacket: RoutePacket): RoutePacket {
            if (routePacket is AsyncBlockPacket<*>) {
                return routePacket
            }
            var movePacket = of(routePacket.routeHeader, routePacket.movePayload())
            movePacket.timerId = routePacket.timerId
            movePacket.timerCallback = routePacket.timerCallback
            return movePacket
        }

        fun of(routePacketMsg: RoutePacketMsg): RoutePacket {
            return RoutePacket(RouteHeader.of(routePacketMsg.routeHeaderMsg), ByteStringPayload(routePacketMsg.message))
        }

        //        fun of(routeHeader: RouteHeader, message: ByteBuf): RoutePacket {
//            return RoutePacket(routeHeader,ProtoPayload(message))
//        }
        fun of(routeHeader: RouteHeader, payload: Payload): RoutePacket {
            return RoutePacket(routeHeader, payload)
        }

        fun of(routeHeader: RouteHeader, message: GeneratedMessageV3): RoutePacket {
            return RoutePacket(routeHeader, ProtoPayload(message))
        }

        fun systemOf(packet: Packet, isBase: Boolean): RoutePacket {
            val header = Header(msgId= packet.msgId)
            val routeHeader = RouteHeader.of(header).apply {
                this.isSystem = true
                this.isBase = isBase
            }
            return RoutePacket(routeHeader, packet.movePayload())
        }

        fun apiOf(
                  packet: Packet,
                  isBase: Boolean, isBackend: Boolean): RoutePacket {
            val header = Header(msgId = packet.msgId)
            val routeHeader = RouteHeader.of(header).apply {
                this.isBase = isBase
                this.isBackend = isBackend
            }
            return RoutePacket(routeHeader, packet.movePayload())
        }

        fun sessionOf(sid: Int,
                      packet: Packet,
                      isBase: Boolean, isBackend: Boolean): RoutePacket {
            val header = Header(msgId = packet.msgId)
            val routeHeader = RouteHeader.of(header).apply {
                this.sid = sid
                this.isBase = isBase
                this.isBackend = isBackend
            }
            return RoutePacket(routeHeader, packet.movePayload())
        }

        fun addTimerOf(
                type: TimerMsg.Type,
                stageId: Long,
                timerId: Long,
                timerCallback: TimerCallback,
                initialDelay: Duration,
                period: Duration,
                count: Int = 0
        ): RoutePacket {
            val header = Header(msgId = TimerMsg.getDescriptor().index)
            val routeHeader = RouteHeader.of(header).apply {
                this.stageId = stageId
                this.isBase = true
            }

            val message = TimerMsg.newBuilder().setType(type).setCount(count)
                    .setInitialDelay(initialDelay.toMillis())
                    .setPeriod(period.toMillis())
                    .build()
            return RoutePacket(routeHeader, ProtoPayload(message)).apply {
                this.timerCallback = timerCallback
                this.timerId = timerId
            }
        }

        fun stageTimerOf(
                stageId: Long,
                timerId: Long,
                timerCallback: TimerCallback
        ): RoutePacket {
            val header = Header(msgId = StageTimer.getDescriptor().index)
            val routeHeader = RouteHeader.of(header)
            return RoutePacket(routeHeader, EmptyPayload()).apply {
                this.routeHeader.stageId = stageId
                this.timerId = timerId
                this.timerCallback = timerCallback
                this.routeHeader.isBase = true
            }
        }

        fun stageOf(
                stageId: Long,
                accountId: Long,
                packet: Packet,
                isBase: Boolean, isBackend: Boolean): RoutePacket {
            val header = Header(msgId = packet.msgId)
            val routeHeader = RouteHeader.of(header).apply {
                this.stageId = stageId
                this.accountId = accountId
                this.isBase = isBase
                this.isBackend = isBackend
            }
            return RoutePacket(routeHeader, packet.movePayload())
        }

        fun replyOf(
                serviceId: Short,
                msgSeq: Short,
                reply: ReplyPacket
        ): RoutePacket {
            val header = Header(msgId = reply.msgId).apply {
                this.serviceId = serviceId
                this.msgSeq = msgSeq
                this.errorCode = reply.errorCode
            }
            val routeHeader = RouteHeader.of(header).apply {
                this.isReply = true
            }
            return RoutePacket(routeHeader, reply.movePayload())
        }

        fun clientOf(serviceId: Short, sid: Int, packet: Packet): RoutePacket {
            val header = Header(msgId = packet.msgId).apply {
                this.serviceId = serviceId
            }
            val routeHeader = RouteHeader.of(header).apply {
                this.sid = sid
                this.forClient = true
            }
            return RoutePacket(routeHeader, packet.movePayload())
        }

        fun writeClientPacketBytes(clientPacket: ClientPacket, buffer: ByteBuf) {
            val body = clientPacket.payload.data()
            val bodySize = body.limit()

            if(bodySize > MAX_PACKET_SIZE){
                throw IOException("body size is over : $bodySize");
            }

            val packetSize = HEADER_SIZE + bodySize

            buffer.capacity(packetSize)
            buffer.writeShort(bodySize)
            buffer.writeShort(clientPacket.serviceId().toInt())
            buffer.writeInt(clientPacket.msgId())
            buffer.writeShort(clientPacket.header.msgSeq.toInt())
            buffer.writeShort(clientPacket.header.errorCode.toInt())
            buffer.writeBytes(clientPacket.payload.data())
        }
    }

    override fun movePayload(): Payload {
        val temp = payload
        payload = EmptyPayload()
        return temp;
    }

    override fun close() {
        this.payload.close()
    }

    fun getPayload(): Payload {
        return this.payload
    }

    fun writeClientPacketBytes(buffer: ByteBuf) {
        val clientPacket = toClientPacket()
        writeClientPacketBytes(clientPacket,buffer)
    }

}

class AsyncBlockPacket<T> private constructor(
    val asyncPostCallback: AsyncPostCallback<T>,
    val result:T,
    routeHeader: RouteHeader
) : RoutePacket(routeHeader, EmptyPayload()) {

    companion object {
        fun<T> of(
            stageId: Long,
            asyncPostCallback: AsyncPostCallback<T>,
            result:T
        ): RoutePacket {
            val header = Header(msgId = AsyncBlock.getDescriptor().index)
            val routeHeader = RouteHeader.of(header)
            return AsyncBlockPacket(
                asyncPostCallback,
                result,
                routeHeader).apply {
                this.routeHeader.stageId = stageId
                this.routeHeader.isBase = true
            }
        }
    }

}