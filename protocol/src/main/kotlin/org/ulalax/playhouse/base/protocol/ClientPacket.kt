package org.ulalax.playhouse.base.protocol
import com.google.protobuf.ByteString
import com.google.protobuf.GeneratedMessageV3
import org.ulalax.playhouse.base.ByteBufferAllocator
import org.ulalax.playhouse.Common.*
import io.netty.buffer.ByteBuf
import io.netty.buffer.EmptyByteBuf
import org.apache.logging.log4j.kotlin.logger
import java.nio.ByteBuffer

class Header constructor(var msgName:String="",var errorCode:Int = 0,var msgSeq:Int=0, var baseErrorCode:Int =0, var serviceId: String=""){
    companion object {
        fun of(headerMsg: HeaderMsg): Header {
            return Header(headerMsg.msgName,headerMsg.errorCode,headerMsg.msgSeq,headerMsg.baseErrorCode,headerMsg.serviceId)
        }
    }
    fun toMsg(): HeaderMsg {
        return HeaderMsg.newBuilder()
            .setServiceId(this.serviceId)
            .setMsgSeq(this.msgSeq)
            .setMsgName(this.msgName)
            .setErrorCode(this.errorCode)
            .setBaseErrorCode(this.baseErrorCode).build()
    }
}

interface Payload : AutoCloseable{

    fun buffer( isCopied: Boolean = false):ByteBuf
    //fun frame():Frame
//    fun retain():Payload
//    fun release():Payload
}

class ProtoPayload : Payload {
    private val log = logger()
    constructor(message: GeneratedMessageV3){
        this.proto = message
    }
    constructor(message: ByteString){
        this.byteString = message
    }

    constructor(message: ByteBuf = EmptyByteBuf(ByteBufferAllocator.allocator)){

//        if(message !is  EmptyByteBuf && !message.isDirect){
//            //copy
//            this.buffer = ByteBufferAllocator.getBuf(message.nioBuffer())
//        }else{
//            this.buffer = message.retainedSlice()
//        }
//        if(message is EmptyByteBuf){
//            this.buffer = message
//        }else{
//            this.buffer = ByteBufferAllocator.getBuf(message.nioBuffer())
//        }
        this.buffer = message

    }

//    constructor(message: ByteBuffer){
//        this.buffer = ByteBufferAllocator.getBuf(message)
//        //this.buffer = Unpooled.wrappedBuffer(message)
////        log.info("constructor buffer refcnt is ${buffer!!.refCnt()}")
//
//    }
//    constructor(frame: Frame){
//        this.frame = frame
//    }


    private var buffer: ByteBuf? = null
    private var byteString:ByteString? = null
    private var proto:GeneratedMessageV3? = null
//    private var frame:Frame? = null

//    override fun buffer():ByteBuffer{
//        return byteBuf().nioBuffer()
//    }
    override fun close() {
        buffer?.apply {
            this.release()
        }

//        frame?.apply {
//            frame!!.close()
//            frame = null
//        }

        buffer = null
        byteString = null
        proto = null
    }

    override fun buffer(isCopied: Boolean): ByteBuf {
        if(buffer == null){
//            frame?.apply {
//                buffer = if(isCopied){
//                    ByteBufferAllocator.getBuf(frame!!.buffer())
//                }else{
//                    Unpooled.wrappedBuffer(frame!!.buffer())
//                }
//            }
            proto?.apply {
                buffer = ByteBufferAllocator.getBuf(proto!!)
            }
            byteString?.apply {
                buffer = ByteBufferAllocator.getBuf(byteString!!)
            }
        }

        if(buffer == null){
            log.error("buffer is null")
        }
        return this.buffer!!
    }

//    fun frame(): Frame {
//        if(frame == null){
//            buffer?.apply {
//                frame = if(buffer is EmptyByteBuf){
//                    Frame()
//                }else{
//                    Frame(buffer!!.nioBuffer())
//                }
//            }
//            proto?.apply {
//                frame = Frame(proto!!.toByteArray())
//            }
//
//        }
//
//        return this.frame!!
//    }

//    fun moveFrame():Frame{
//        var temp = frame()
//        this.frame = null
//        return temp
//    }

    fun proto(): GeneratedMessageV3? {
        return proto
    }

}

class ClientPacket private constructor(val header: Header,private var payload: Payload) : BasePacket{
    companion object{
//        fun messageOf(packetMsg: PacketMsg): ClientPacket {
//            return ClientPacket(Header.of(packetMsg.headerMsg), packetMsg.message)
//        }

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
        payload = ProtoPayload()
        return temp;
    }

    override fun buffer(): ByteBuffer {
        return this.payload.buffer().nioBuffer()
    }
    fun serviceId():String {
        return header.serviceId
    }
    fun msgName():String {
        return header.msgName
    }
//    fun toMsg():PacketMsg{
//        return PacketMsg.newBuilder()
//            .setHeaderMsg(header.toMsg())
//            .setMessage(message).build()
//    }

    fun toReplyPacket():ReplyPacket{
        return ReplyPacket(header.errorCode,header.msgName,movePayload())
    }

    fun toByteBuf(): ByteBuf {

        val header = header.toMsg()
        val headerSize = header.serializedSize
        val body = (payload as ProtoPayload).buffer(false)
        val packetSize = 1 + headerSize+body.capacity()

        val compositeByteBuf = ByteBufferAllocator.getCompositeBuffer()
        val headBuffer = ByteBufferAllocator.getBuf(1+headerSize)

        headBuffer.writeByte(headerSize)
        headBuffer.writeBytes(header.toByteArray())

        compositeByteBuf.addComponent(headBuffer)
        compositeByteBuf.addComponent(body)
        compositeByteBuf.writerIndex(packetSize)




        return compositeByteBuf

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

