package org.ulalax.playhouse.base.communicator.zmq

import io.netty.buffer.Unpooled
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.logging.log4j.kotlin.logger
import org.ulalax.playhouse.base.Server
import org.ulalax.playhouse.base.Server.*
import org.ulalax.playhouse.base.communicator.message.RouteHeader
import org.ulalax.playhouse.base.communicator.message.RoutePacket
import org.ulalax.playhouse.base.protocol.ProtoPayload
import org.zeromq.ZFrame
import org.zeromq.ZMQ
import org.zeromq.ZMsg
import java.nio.ByteBuffer


class JZmqSocket(override val bindEndpoint:String) : ZmqSocket {
    private val log = logger()
    private val ctx = ZMQ.context(1)
    private val socket = ctx.socket(ZMQ.ROUTER)

    init {
        socket.identity = bindEndpoint.toByteArray()
        socket.setRouterMandatory(true)
        socket.immediate = true
        socket.setRouterHandOver(true)
        socket.backlog = 1000
        socket.linger = 0

        socket.setTCPKeepAlive(1)
        socket.tcpKeepAliveCount = 5
        socket.tcpKeepAliveInterval = 1

        socket.sendBufferSize = 64*1024//1024000
        socket.receiveBufferSize = 64*1024//1024000
        socket.rcvHWM = 0
        socket.sndHWM = 0
        socket.hwm = 0

        log.info("zmqsocket init bindEndpoint:$bindEndpoint")
    }

    override fun bind(){
        socket.bind(bindEndpoint)
    }
    override fun send(target: String, routePacket: RoutePacket){
        socket.send(target,ZMQ.SNDMORE)
        socket.send(routePacket.routeHeader.toByteArray(),ZMQ.SNDMORE)
        //socket.send(routePacket.message.nioBuffer().array(),ZMQ.DONTWAIT);
        val byteBuf = routePacket.getPayload().buffer()
        if (byteBuf.isDirect) {
            val nioBuffer: ByteBuffer = byteBuf.nioBuffer(0, byteBuf.writerIndex())
            socket.sendByteBuffer(nioBuffer, ZMQ.DONTWAIT)
            //socket.sendZeroCopy(nioBuffer,byteBuf.writerIndex(),ZMQ.DONTWAIT);
        } else {
            //val nioBuffer: ByteBuffer = byteBuf.nioBuffer(byteBuf.arrayOffset(), byteBuf.readableBytes())
            //socket.sendByteBuffer(nioBuffer,ZMQ.DONTWAIT)
            //socket.send(byteBuf.array(),0,byteBuf.readableBytes(),ZMQ.DONTWAIT);
            socket.send(byteBuf.array(), byteBuf.arrayOffset(), byteBuf.writerIndex(), ZMQ.DONTWAIT)
        }

    }

    override fun connect(target: String) {
        socket.connect(target)
    }

    override fun receive(): RoutePacket? {
        try{
            val msg: ZMsg = receiveMessage() ?: return null
            val target = msg.popString()
            val header = RouteHeaderMsg.parseFrom(msg.pop().data)
            val message = msg.pop().data
            val routePacket =  RoutePacket.of(RouteHeader.of(header), ProtoPayload(Unpooled.wrappedBuffer(message)))
            //val routePacket =  RoutePacket.of(RouteHeader.of(header), ProtoPayload(ByteBufferAllocator.getBuf(message)))
            routePacket.routeHeader.from = target
            return routePacket
        }catch (e:Exception){
            log.error(ExceptionUtils.getStackTrace(e))
            return null;
        }

    }

    private fun receiveMessage(): ZMsg? {
        var msg = ZMsg()
        while (true) {
            val zFrame = ZFrame.recvFrame(socket, ZMQ.DONTWAIT)
            if (zFrame == null || !zFrame.hasData()) {
                // If receive failed or was interrupted
                msg.destroy()
                return null
            }
            msg.add(zFrame)
            if (!zFrame.hasMore()) break
        }
        return msg
    }

    override fun disconnect(endpoint: String) {
        socket.disconnect(endpoint)
    }

    override fun close() {
        socket.close()
    }
}