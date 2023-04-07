package org.ulalax.playhouse.service

import org.ulalax.playhouse.communicator.*

class BaseSystemPanel(
        private val serverInfoCenter: ServerInfoCenter,
        private val clientCommunicator: ClientCommunicator,

        ) : SystemPanel {

    lateinit var communicator: Communicator

    override fun randomServerInfo(serviceId:Short): ServerInfo {
        return serverInfoCenter.findRoundRobinServer(serviceId)
    }

    override fun serverInfo(endpoint: String): ServerInfo {
        return serverInfoCenter.findServer(endpoint)
    }

    override fun serverList(): List<ServerInfo> {
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


}