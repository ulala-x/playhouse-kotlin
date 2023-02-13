package org.ulalax.playhouse.service

import org.ulalax.playhouse.communicator.CommunicateClient
import org.ulalax.playhouse.communicator.message.RoutePacket

class SpyCommunicateClient(val resultCollector:MutableList<RoutePacket> ) : CommunicateClient {
    override fun connect(endpoint: String) {
    }

    override fun send(endpoint: String, routePacket: RoutePacket) {
        resultCollector.add(routePacket)
    }

    override fun communicate() {
    }

    override fun disconnect(endpoint: String) {
    }
}