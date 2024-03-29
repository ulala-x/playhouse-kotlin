package org.ulalax.playhouse.communicator.socket

import org.apache.commons.lang3.exception.ExceptionUtils
import LOG
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.ulalax.playhouse.communicator.ConstOption
import org.ulalax.playhouse.communicator.message.FramePayload
import org.ulalax.playhouse.communicator.message.PreAllocByteArrayOutputStream
import org.ulalax.playhouse.communicator.message.RouteHeader
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.protocol.Server
import org.zeromq.SocketType
import org.zeromq.ZFrame
import org.zeromq.ZMessage
import org.zeromq.ZSocket




class ZmqJPlaySocket  (
    socketConfig:SocketConfig, override val id:String) : PlaySocket {

    private val socket = ZSocket(SocketType.ROUTER)
    private val buffer:ByteBuf = Unpooled.buffer(ConstOption.MAX_PACKET_SIZE)

    init {
        socket.routingId(id.toByteArray())
        socket.immediate(true)
        socket.routerHandOver(true)
        socket.backlog(socketConfig.backLog)
        socket.linger(socketConfig.linger)
        socket.tcpKeepAlive(1)
//        socket.tcpKeepAliveCount(5)
//        socket.tcpKeepAliveInterval(1)
        socket.sendBufferSize(socketConfig.sendBufferSize)
        socket.receiveBufferSize(socketConfig.receiveBufferSize)
        socket.receiveHighWaterMark(socketConfig.receiveHighWatermark)
        socket.sendHighWaterMark(socketConfig.sendHighWatermark)
        socket.routerMandatory(true)
   }

    override fun bind(){
        socket.bind(id)
        LOG.info("socket bind $id",this)
    }
    override fun send(target: String, routePacket: RoutePacket){

        LOG.trace("sendTo:$target, packetInfo:${routePacket.routeHeader}",this)

        var message = ZMessage()
        val payload = routePacket.getPayload()

        val frame:ZFrame = if (payload is FramePayload){
            payload.frame
        } else {
            buffer.clear()
            if (routePacket.forClient()) {
                routePacket.writeClientPacketBytes(buffer)
            } else {
                buffer.writeBytes(payload.data())
            }
            ZFrame(buffer.array(), buffer.arrayOffset()+buffer.readerIndex(),buffer.readableBytes())
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

