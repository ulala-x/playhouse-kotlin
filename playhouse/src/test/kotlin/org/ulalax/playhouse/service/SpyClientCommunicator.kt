package org.ulalax.playhouse.service

import org.ulalax.playhouse.communicator.ClientCommunicator
import org.ulalax.playhouse.communicator.message.RoutePacket

class SpyClientCommunicator(private val resultCollector:MutableList<RoutePacket> ) : ClientCommunicator {
    override fun connect(endpoint: String) {
    }

    override fun send(endpoint: String, routePacket: RoutePacket) {
        resultCollector.add(routePacket)
    }

    override fun communicate() {
    }

    override fun disconnect(endpoint: String) {
    }

    override fun stop() {
    }
}