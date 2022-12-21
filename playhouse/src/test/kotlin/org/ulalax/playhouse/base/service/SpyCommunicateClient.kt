package org.ulalax.playhouse.base.service

import org.ulalax.playhouse.base.communicator.CommunicateClient
import org.ulalax.playhouse.base.communicator.message.RoutePacket

class SpyCommunicateClient(val resultCollector:MutableList< RoutePacket> ) : CommunicateClient {
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