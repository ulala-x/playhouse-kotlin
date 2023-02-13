package org.ulalax.playhouse.communicator.message

import com.google.protobuf.GeneratedMessageV3
import org.ulalax.playhouse.protocol.Common.HeaderMsg
import org.ulalax.playhouse.protocol.Server.*
import org.ulalax.playhouse.protocol.*
import org.ulalax.playhouse.service.AsyncPostCallback
import org.ulalax.playhouse.service.play.base.TimerCallback
import org.zeromq.ZFrame
import java.time.Duration


class RouteHeader private constructor(val header: Header,
                                      var sid:Int=-1,
                                      var sessionInfo: String = "",
                                      var isSystem: Boolean = false,
                                      var isBase:Boolean = false,
                                      var isBackend:Boolean = false,
                                      var isReply:Boolean = false,
                                      var accountId:Long = 0,
                                      var stageId:Long = 0
){

    var from: String = ""
    fun toMsg(): RouteHeaderMsg {
        return RouteHeaderMsg.newBuilder()
            .setHeaderMsg(header.toMsg())
            .setSid(sid)
            .setSessionInfo(sessionInfo)
            .setIsSystem(isSystem)
            .setIsBase(isBase)
            .setIsBackend(isBackend)
            .setIsReply(isReply)
            .setAccountId(accountId)
            .setStageId(stageId)
            .build()
    }

    companion object {
        fun of(headerMsg: RouteHeaderMsg): RouteHeader {
            return RouteHeader(
                Header.of(headerMsg.headerMsg),
                headerMsg.sid, headerMsg.sessionInfo,
                headerMsg.isSystem,headerMsg.isBase,headerMsg.isBackend,headerMsg.isReply,
                headerMsg.accountId,headerMsg.stageId,
            )
        }

        fun of(header: HeaderMsg): RouteHeader {
            return RouteHeader(Header.of(header))
        }
        fun of(header: Header): RouteHeader {
            return RouteHeader(header)
        }
        fun timerOf(stageId: Long,msgName:String): RouteHeader {
            return RouteHeader(Header(msgName)).apply {
                this.stageId = stageId
            }
        }
    }


    fun toByteArray(): ByteArray {
        return toMsg().toByteArray()
    }

    fun msgName(): String {
        return this.header.msgName
    }

}

open class RoutePacket protected constructor(val routeHeader: RouteHeader, private var payload: Payload) : BasePacket {

    var timerId:Long = 0
    var timerCallback: TimerCallback = {}

    fun msgName():String { return  routeHeader.header.msgName }
    fun serviceId():String {return routeHeader.header.serviceId}
    fun isBackend():Boolean{return routeHeader.isBackend}


//    fun toMsg(): Plbase.RoutePacketMsg {
//        return Plbase.RoutePacketMsg.newBuilder()
//            .setRouteHeaderMsg(routeHeader.toMsg())
//            .setMessage(message).build()
//    }
    override fun frame(): ZFrame {
        return payload.frame()
    }

    override fun data(): ByteArray {
        return frame().data();
    }

    fun toReplyPacket(): ReplyPacket {
        return ReplyPacket(routeHeader.header.errorCode,routeHeader.msgName(),movePayload())
    }

    fun toClientPacket(): ClientPacket {
//        val packetMsg = PacketMsg.newBuilder()
//            .setHeaderMsg(routeHeader.header.toMsg())
//            .setMessage(message).build()
        return ClientPacket.of(routeHeader.header.toMsg(),movePayload())
    }

    fun toPacket(): Packet {
        return Packet(msgName(),movePayload())
    }

    fun isBase(): Boolean {
        return routeHeader.isBase
    }

    fun accountId(): Long {
        return routeHeader.accountId
    }

    fun setMsgSeq(msgSeq: Int) {
        routeHeader.header.msgSeq = msgSeq
    }

    fun isRequest(): Boolean {
        return routeHeader.header.msgSeq != 0
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


    companion object {
        fun moveOf(routePacket: RoutePacket): RoutePacket {
            if (routePacket is AsyncBlockPacket<*>){
                return routePacket
            }
            var movePacket = of(routePacket.routeHeader,routePacket.movePayload())
            movePacket.timerId = routePacket.timerId
            movePacket.timerCallback = routePacket.timerCallback
            return movePacket
        }

        fun of(routePacketMsg: RoutePacketMsg): RoutePacket {
            return RoutePacket(RouteHeader.of(routePacketMsg.routeHeaderMsg), ProtoPayload(routePacketMsg.message))
        }
//        fun of(routeHeader: RouteHeader, message: ByteBuf): RoutePacket {
//            return RoutePacket(routeHeader,ProtoPayload(message))
//        }
        fun of(routeHeader: RouteHeader, payload: Payload): RoutePacket {
            return RoutePacket(routeHeader,payload)
        }

        fun of(routeHeader: RouteHeader, message: GeneratedMessageV3): RoutePacket {
            return RoutePacket(routeHeader, ProtoPayload(message))
        }

        fun systemOf(packet: Packet, isBase: Boolean): RoutePacket {
            val header = Header(packet.msgName)
            val routeHeader = RouteHeader.of(header).apply {
                this.isSystem = true
                this.isBase = isBase
            }
            return RoutePacket(routeHeader,packet.movePayload())
        }
        fun  apiOf(sessionInfo: String,
                   packet: Packet,
                   isBase: Boolean, isBackend: Boolean): RoutePacket {
            val header = Header(packet.msgName)
            val routeHeader = RouteHeader.of(header).apply {
                this.sessionInfo = sessionInfo
                this.isBase = isBase
                this.isBackend = isBackend
            }
            return RoutePacket(routeHeader,packet.movePayload())
        }
        fun sessionOf(sid:Int,
                      packet: Packet,
                      isBase: Boolean, isBackend: Boolean): RoutePacket {
            val header = Header(packet.msgName)
            val routeHeader = RouteHeader.of(header).apply {
                this.sid = sid
                this.isBase = isBase
                this.isBackend = isBackend
            }
            return RoutePacket(routeHeader,packet.movePayload())
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
            val header = Header(TimerMsg.getDescriptor().name)
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

        fun StageTimerOf(
            stageId: Long,
            timerId: Long,
            timerCallback: TimerCallback
        ): RoutePacket {
            val header = Header(StageTimer.getDescriptor().name)
            val routeHeader = RouteHeader.of(header)
            return RoutePacket(routeHeader, ProtoPayload()).apply {
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
            val header = Header(packet.msgName)
            val routeHeader = RouteHeader.of(header).apply {
                this.stageId = stageId
                this.accountId = accountId
                this.isBase = isBase
                this.isBackend = isBackend
            }
            return RoutePacket(routeHeader,packet.movePayload())
        }

        fun replyOf(
            serviceId: String,
            msgSeq: Int,
            reply: ReplyPacket
        ): RoutePacket {
            val header = Header(reply.msgName).apply {
                this.serviceId = serviceId
                this.msgSeq = msgSeq
                this.errorCode = reply.errorCode
            }
            val routeHeader = RouteHeader.of(header).apply {
                this.isReply = true
            }
            return RoutePacket(routeHeader,reply.movePayload())
        }

        fun clientOf(serviceId: String, sid: Int, packet: Packet): RoutePacket {
            val header = Header(packet.msgName).apply {
                this.serviceId = serviceId
            }
            val routeHeader = RouteHeader.of(header).apply {
                this.sid = sid
            }
            return RoutePacket(routeHeader,packet.movePayload())
        }
    }

    override fun movePayload(): Payload {
        val temp = payload
        payload = ProtoPayload()
        return temp;
    }

    override fun close() {
        this.payload.close()
    }

    fun getPayload(): Payload {
        return this.payload
    }

}

class AsyncBlockPacket<T> private constructor(
    val asyncPostCallback: AsyncPostCallback<T>,
    val result:T,
    routeHeader: RouteHeader
) : RoutePacket(routeHeader, ProtoPayload()) {

    companion object {
        fun<T> of(
            stageId: Long,
            asyncPostCallback: AsyncPostCallback<T>,
            result:T
        ): RoutePacket {
            val header = Header(AsyncBlock.getDescriptor().name)
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