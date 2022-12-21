//package org.ulalax.playhouse.base.communicator.zmq
//
//import org.ulalax.playhouse.base.Plbase
//import org.ulalax.playhouse.base.communicator.message.RouteHeader
//import org.ulalax.playhouse.base.communicator.message.RoutePacket
//import org.ulalax.playhouse.base.protocol.ProtoPayload
//import org.apache.commons.lang3.exception.ExceptionUtils
//import org.apache.logging.log4j.kotlin.logger
//import org.zeromq.jmq.Frame
//import org.zeromq.jmq.Message
//import org.zeromq.jmq.Socket
//import org.zeromq.jmq.SocketType
//
//
//class JmqSocket(val bindEndpoint:String) {
//    private val log = logger()
//    private val socket = Socket(SocketType.Router)
//
//    init {
//        val option = socket.option
//        option.routingId(bindEndpoint)
//        option.immediate(true)
//        option.routerHandOver(true)
//        option.backlog(1000)
//        option.linger(0)
//        option.tcpKeepAlive(1)
//        option.tcpKeepAliveCount(5)
//        option.tcpKeepAliveInterval(1)
//        option.sendBufferSize(64*1000)
//        option.receiveBufferSize(64*1000)
//        option.receiveHighWaterMark(0)
//        option.sendHighWaterMark(0)
//
//
////        socket.setRouterMandatory(true)
////        socket.immediate = true
////        socket.setRouterHandOver(true)
////        socket.backlog = 1000
////        socket.linger = 0
////
////        socket.setTCPKeepAlive(1)
////        socket.tcpKeepAliveCount = 5
////        socket.tcpKeepAliveInterval = 1
////
////        socket.sendBufferSize = 1024000
////        socket.receiveBufferSize = 1024000
////        socket.rcvHWM = 0
////        socket.sndHWM = 0
////        socket.hwm = 0
//
//        log.info("zmqsocket init bindEndpoint:$bindEndpoint")
//    }
//
//    fun bind(){
//        socket.bind(bindEndpoint)
//    }
//    fun send(target: String, routePacket: RoutePacket){
//
//        var message = Message()
//        val payload = routePacket.getPayload() as ProtoPayload
//
//        message.use{
//            message.push(Frame(target))
//            message.push(Frame(routePacket.routeHeader.toByteArray()))
//            message.push(payload.moveFrame())
//            //message.push(Frame.zeroCopyOf(routePacket.getPayload().buffer().retainedSlice().nioBuffer()))
//            //message.push(Frame(routePacket.getPayload().buffer().nioBuffer()))
//            this.socket.send(message,true)
//        }
//
//
//
////            if (byteBuf.isDirect) {
////                val nioBuffer: ByteBuffer = byteBuf.nioBuffer(0, byteBuf.writerIndex())
////                message.push(Frame(nioBuffer))
////                //socket.sendByteBuffer(nioBuffer, ZMQ.DONTWAIT)
////                //socket.sendZeroCopy(nioBuffer,byteBuf.writerIndex(),flag);
////            } else {
////                //socket.send(byteBuf.array(),0,byteBuf.readableBytes(),flag);
////                //socket.send(byteBuf.array(), byteBuf.arrayOffset(), byteBuf.writerIndex(), ZMQ.DONTWAIT)
////                val nioBuffer =  byteBuf.slice(byteBuf.arrayOffset(),byteBuf.writerIndex()).nioBuffer()
////                message.push(Frame(nioBuffer))
////            }
//    }
//
//    fun connect(target: String) {
//        socket.connect(target)
//    }
//
//    fun receive(): RoutePacket? {
//        try{
//            val msg: Message = receiveMessage()
//            if(msg.parts() == 0){
//                return null;
//            }
//            msg.use {
//                val target = msg.frame(0).string()
//                val header = Plbase.RouteHeaderMsg.parseFrom(msg.buffer(1))
//                //val body = msg.buffer(2)
//                val body = msg.move(2)
//
//                val routePacket =  RoutePacket.of(RouteHeader.of(header), ProtoPayload(body))
//                routePacket.routeHeader.from = target
//                return routePacket
//            }
//
//        }catch (e:Exception){
//            log.error(ExceptionUtils.getStackTrace(e))
//            return null;
//        }
//
//    }
//
//    private fun receiveMessage(): Message {
//        var msg = Message()
//        socket.receive(msg,true)
//        return msg
//    }
//
//    fun disconnect(endpoint: String) {
//        socket.disconnect(endpoint)
//    }
//
//    fun close() {
//        socket.close()
//    }
//}
//
