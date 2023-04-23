package org.ulalax.playhouse.service

import org.ulalax.playhouse.communicator.*

class XSystemPanel(
        private val serverInfoCenter: ServerInfoCenter,
        private val clientCommunicator: ClientCommunicator,
        private val nodeId:Int
        ) : SystemPanel {

    lateinit var communicator: Communicator
    private val idGenerator = UniqueIdGenerator(nodeId)

    override fun getServerInfoByService(serviceId:Short): ServerInfo {
        return serverInfoCenter.findRoundRobinServer(serviceId)
    }

    override fun getServerInfoByEndpoint(endpoint: String): ServerInfo {
        return serverInfoCenter.findServer(endpoint)
    }

    override fun getServers(): List<ServerInfo> {
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

    override fun getServerState(): ServerState {
        return communicator.serverState()
    }

    override fun generateUUID(): Long {
        return idGenerator.nextId()
    }


}