package org.ulalax.playhouse.service

import org.ulalax.playhouse.communicator.*

class BaseSystemPanel(
        private val serverInfoCenter: ServerInfoCenter,
        private val clientCommunicator: ClientCommunicator,

        ) : SystemPanel {

    lateinit var communicator: Communicator

    override fun randomServerInfo(serviceId:String): XServerInfo {
        return serverInfoCenter.findRoundRobinServer(serviceId)
    }

    override fun serverInfo(endpoint: String): XServerInfo {
        return serverInfoCenter.findServer(endpoint)
    }

    override fun serverList(): List<XServerInfo> {
        return serverInfoCenter.getServerList()
    }

    override fun pause() {
        communicator.pause()
    }

    override fun resume() {
        communicator.resume()
    }

    override fun shutdown() {
        communicator.stop()
    }

    override fun serverState(): ServerState {
        return communicator.serverState()
    }

//    override fun sendToServer(endpoint: String, packet: Packet) {
//        communicateClient.send(endpoint, RoutePacket.systemOf(packet,false))
//    }
//
//    override fun callToServer(endpoint: String, packet: Packet): ReplyPacket {
//        val msgSeq = this.reqCache.getSequence()
//        val routePacket = RoutePacket.systemOf(packet,false).apply { routeHeader.header.msgSeq = msgSeq }
//        communicateClient.send(endpoint,routePacket)
//        val future = CompletableFuture<ReplyPacket>()
//        this.reqCache.put(msgSeq, ReplyObject(future = future))
//        return future.get()
//    }
}