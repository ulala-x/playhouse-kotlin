package org.ulalax.playhouse.communicator.zmq

import org.ulalax.playhouse.protocol.ProtoPayload
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.logging.log4j.kotlin.logger
import org.ulalax.playhouse.communicator.message.RouteHeader
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.protocol.Server
import org.zeromq.SocketType
import org.zeromq.ZFrame
import org.zeromq.ZMessage
import org.zeromq.ZSocket


class ZmqJSocket(val bindEndpoint:String) {
    private val log = logger()
    private val socket = ZSocket(SocketType.ROUTER)

    init {
        socket.routingId(bindEndpoint.toByteArray())
        socket.immediate(true)
        socket.routerHandOver(true)
        socket.backlog(1000)
        socket.linger(0)
        socket.tcpKeepAlive(1)
        socket.tcpKeepAliveCount(5)
        socket.tcpKeepAliveInterval(1)
        socket.sendBufferSize(64*1000)
        socket.receiveBufferSize(64*1000)
        socket.receiveHighWaterMark(0)
        socket.sendHighWaterMark(0)


        socket.routerMandatory(true)
//        socket.immediate = true
//        socket.setRouterHandOver(true)
//        socket.backlog = 1000
//        socket.linger = 0
//
//        socket.setTCPKeepAlive(1)
//        socket.tcpKeepAliveCount = 5
//        socket.tcpKeepAliveInterval = 1
//
//        socket.sendBufferSize = 1024000
//        socket.receiveBufferSize = 1024000
//        socket.rcvHWM = 0
//        socket.sndHWM = 0
//        socket.hwm = 0

        log.info("zmqsocket init bindEndpoint:$bindEndpoint")
    }

    fun bind(){
        socket.bind(bindEndpoint)
    }
    fun send(target: String, routePacket: RoutePacket){

        var message = ZMessage()
        val payload = routePacket.getPayload() as ProtoPayload

        message.use{
            message.add(ZFrame(target.toByteArray()))
            message.add(ZFrame(routePacket.routeHeader.toByteArray()))
            message.add(payload.frame())
            this.socket.send(message,true)
        }
    }

    fun connect(target: String) {
        socket.connect(target)
    }

    fun receive(): RoutePacket? {
        try{
            val msg: ZMessage = receiveMessage()
            if(msg.size == 0){
                return null;
            }
            msg.use {
                val target = String(msg.frame(0).data())
                val header = Server.RouteHeaderMsg.parseFrom(msg.data(1))
                val body = msg.removeAt(2)

                val routePacket =  RoutePacket.of(RouteHeader.of(header), ProtoPayload(body))
                routePacket.routeHeader.from = target
                return routePacket
            }

        }catch (e:Exception){
            log.error(ExceptionUtils.getStackTrace(e))
            return null;
        }

    }

    private fun receiveMessage(): ZMessage {
        var msg = ZMessage()
        socket.receive(msg,true)
        return msg
    }

    fun disconnect(endpoint: String) {
        socket.disconnect(endpoint)
    }

    fun close() {
        socket.close()
    }
}

