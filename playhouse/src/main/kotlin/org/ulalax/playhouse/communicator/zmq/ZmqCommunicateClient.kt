package org.ulalax.playhouse.communicator.zmq


import org.ulalax.playhouse.communicator.CommunicateClient
import org.ulalax.playhouse.communicator.SendBucket
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.logging.log4j.kotlin.logger
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArraySet

class ZmqCommunicateClient(private val clientSocket: ZmqJSocket) : CommunicateClient {
    private val log = logger()

    private val connected:MutableSet<String> = CopyOnWriteArraySet()
    private val connectTarget: Queue<String> = ConcurrentLinkedQueue()
    private val disconnected:MutableSet<String> = CopyOnWriteArraySet()
    private val disconnectTarget: Queue<String> = ConcurrentLinkedQueue()
    private val sendBucket = SendBucket()


    override fun connect(endpoint: String) {
        if(connected.contains(endpoint)) return
        connected.add(endpoint)
        connectTarget.add(endpoint)
    }
    override fun disconnect(endpoint: String) {
        if(disconnected.contains(endpoint)) return
        disconnected.add(endpoint)
        disconnectTarget.add(endpoint)
    }

    override fun send(endpoint: String, routePacket: RoutePacket) {
        sendBucket.add(endpoint,routePacket)
    }

    override fun communicate() {
        makeConnect()
        makeDisconnect()
        sendBucket.get().forEach{ (target, packets) -> communicate(target,packets) }
    }


    private fun communicate(target: String, routePackets: Queue<RoutePacket>) {
        var routePacket: RoutePacket? = routePackets.poll() ?: return

        try {
            while (routePacket != null) {
                routePacket.use {
                    clientSocket.send(target, it)
                }
                routePacket = routePackets.poll()
            }
        }catch (e:Exception){
            log.error("${clientSocket.bindEndpoint} socket send error : $target,${routePacket?.msgName()?:""}")
            log.error(ExceptionUtils.getStackTrace(e))
        }
    }

    private fun makeConnect() {
        var endpoint = this.connectTarget.poll()
        while(endpoint!=null){
            log.info("${clientSocket.bindEndpoint} socket connect with $endpoint")
            this.clientSocket.connect(endpoint)
            this.disconnected.remove(endpoint)
            endpoint = this.connectTarget.poll()
        }
    }
    private fun makeDisconnect() {
        var endpoint = this.disconnectTarget.poll()
        while(endpoint!=null){
            if(this.connected.contains(endpoint)){
                log.info("${clientSocket.bindEndpoint} socket disconnect with $endpoint")
                this.clientSocket.disconnect(endpoint)
                this.connected.remove(endpoint)
            }
            endpoint = this.disconnectTarget.poll()
        }
    }


}