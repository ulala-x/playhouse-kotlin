package org.ulalax.playhouse.communicator.socket

import org.apache.commons.lang3.exception.ExceptionUtils
import org.ulalax.playhouse.LOG
import org.ulalax.playhouse.communicator.ConstOption
import org.ulalax.playhouse.communicator.message.FramePayload
import org.ulalax.playhouse.communicator.message.RouteHeader
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.protocol.Server
import org.zeromq.SocketType
import org.zeromq.ZFrame
import org.zeromq.ZMessage
import org.zeromq.ZSocket
import java.io.OutputStream

class PreAllocByteArrayOutputStream(private val buffer: ByteArray) : OutputStream() {
    private var position: Int = 0

    @Throws(IndexOutOfBoundsException::class)
    override fun write(byte: Int) {
        if (position >= buffer.size) {
            throw IndexOutOfBoundsException("Buffer is full")
        }
        buffer[position++] = byte.toByte()
    }

    fun writtenDataLength(): Int {
        return position
    }

    fun reset() {
        position = 0
    }

    fun writeByte(headerSize: Int) {
        write(headerSize)
    }

    fun writeShort(value: Int) : Int {
        val startIndex = position
        replaceShort(position,value)
        position += 2
        return startIndex
    }

    fun replaceShort(index:Int,value:Int) {
        var tempIndex = index
        buffer[tempIndex++] = (value ushr 8).toByte()
        buffer[tempIndex++] = (value and 0xFF).toByte()
    }

    fun getShort(index:Int): Int {
        val byteArray = buffer.sliceArray(index until index+2)
        require(byteArray.size == 2) { "ByteArray size should be 4 for Int conversion" }
        return  (byteArray[0].toInt() and 0xFF shl 8) or (byteArray[1].toInt() and 0xFF)
    }

    fun array(): ByteArray {
        return buffer.sliceArray(0 until position)
    }

}



class ZmqJPlaySocket  (override val id:String,
        ) : PlaySocket {

    private val socket = ZSocket(SocketType.ROUTER)
    private val outputStream = PreAllocByteArrayOutputStream(ByteArray(ConstOption.MAX_PACKET_SIZE))

    init {
        socket.routingId(id.toByteArray())
        socket.immediate(true)
        socket.routerHandOver(true)
        socket.backlog(1000)
        socket.linger(0)
        socket.tcpKeepAlive(1)
//        socket.tcpKeepAliveCount(5)
//        socket.tcpKeepAliveInterval(1)
        socket.sendBufferSize(1024*1024)
        socket.receiveBufferSize(1024*1024)
        socket.receiveHighWaterMark(1000000)
        socket.sendHighWaterMark(1000000)
        socket.routerMandatory(true)
   }

    override fun bind(){
        socket.bind(id)
        LOG.info("socket bind $id",this)
    }
    override fun send(target: String, routePacket: RoutePacket){

        var message = ZMessage()
        val payload = routePacket.getPayload()

        val frame:ZFrame = if (payload is FramePayload){
            payload.frame
        } else {
            outputStream.reset()
            if (routePacket.forClient()) {
               ZFrame(routePacket.getClientPacketBytes(outputStream))
            } else {
                payload.output(outputStream)
                ZFrame(outputStream.array(), 0, outputStream.writtenDataLength())
            }
        }


        message.use{
            message.add(ZFrame(target.toByteArray()))
            message.add(ZFrame(routePacket.routeHeader.toByteArray()))
            message.add(frame)
            this.socket.send(message,true)
        }
    }

    override fun connect(endpoint: String) {
        socket.connect(endpoint)
    }

    override fun receive(): RoutePacket? {
        try{
            socket.receive(true).use {msg->
                msg.use {
                    when (msg.size) {
                        0 -> {
                            return null
                        }
                        3 -> {
                            val target = String(msg.frame(0).data())
                            val header = Server.RouteHeaderMsg.parseFrom(msg.data(1))
                            val body = msg.removeAt(2)

                            val routePacket =  RoutePacket.of(RouteHeader.of(header), FramePayload(body))
                            routePacket.routeHeader.from = target
                            return routePacket
                        }
                        else -> {
                            LOG.error("message size is invalid ${msg.size}",this)
                            return null
                        }
                    }
                }
            }

        }catch (e:Exception){
            LOG.error(ExceptionUtils.getStackTrace(e),this,e)
            return null;
        }
    }

    override fun disconnect(endpoint: String) {
        socket.disconnect(endpoint)
    }

    override fun close() {
        socket.close()
    }
}

