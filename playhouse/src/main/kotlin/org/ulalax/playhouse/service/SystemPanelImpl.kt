package org.ulalax.playhouse.service

import org.ulalax.playhouse.communicator.*

class SystemPanelImpl(
    private val serverInfoCenter: ServerInfoCenter,
    private val communicateClient: CommunicateClient,
//    private val reqCache: RequestCache,

    ) : SystemPanel {

    lateinit var communicator: Communicator

    override fun randomServerInfo(serviceId:String): ServerInfoImpl {
        return serverInfoCenter.findRoundRobinServer(serviceId)
    }

    override fun serverInfo(endpoint: String): ServerInfoImpl {
        return serverInfoCenter.findServer(endpoint)
    }

    override fun serverList(): List<ServerInfoImpl> {
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