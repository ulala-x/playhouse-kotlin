package org.ulalax.playhouse.communicator

import org.ulalax.playhouse.communicator.message.RoutePacket

interface CommunicateListener {
    fun onReceive(routePacket: RoutePacket)
}

