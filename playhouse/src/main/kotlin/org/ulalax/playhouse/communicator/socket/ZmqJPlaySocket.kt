package org.ulalax.playhouse.communicator.socket

import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.logging.log4j.kotlin.logger
import org.ulalax.playhouse.communicator.message.XPayload
import org.ulalax.playhouse.communicator.message.RouteHeader
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.protocol.Server
import org.zeromq.SocketType
import org.zeromq.ZFrame
import org.zeromq.ZMessage
import org.zeromq.ZSocket


class ZmqJPlaySocket  (override val id:String) : PlaySocket {
    private val log = logger()
    private val socket = ZSocket(SocketType.ROUTER)

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
        log.info("socket bind $id")
    }
    override fun send(target: String, routePacket: RoutePacket){

        var message = ZMessage()
        val payload = routePacket.getPayload() as XPayload

        message.use{
            message.add(ZFrame(target.toByteArray()))
            message.add(ZFrame(routePacket.routeHeader.toByteArray()))
            message.add(payload.frame())
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

                            val routePacket =  RoutePacket.of(RouteHeader.of(header), XPayload(body))
                            routePacket.routeHeader.from = target
                            return routePacket
                        }
                        else -> {
                            log.error("message size is invalid ${msg.size}")
                            return null
                        }
                    }
                }
            }

        }catch (e:Exception){
            log.error(ExceptionUtils.getStackTrace(e))
            return null;
        }

    }

//    private fun receiveMessage(): ZMessage? {
//        var msg = ZMessage()
//        if(socket.receive(msg,true)){
//            return msg
//        }
//        return null
//    }

    override fun disconnect(endpoint: String) {
        socket.disconnect(endpoint)
    }

    override fun close() {
        socket.close()
    }
}

