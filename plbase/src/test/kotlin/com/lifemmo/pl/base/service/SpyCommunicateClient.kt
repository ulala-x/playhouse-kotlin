package com.lifemmo.pl.base.service

import com.lifemmo.pl.base.communicator.CommunicateClient
import com.lifemmo.pl.base.communicator.message.RoutePacket

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