package org.ulalax.playhouse.base.communicator

import org.ulalax.playhouse.base.communicator.message.RoutePacket

interface CommunicateListener {
    fun onReceive(routePacket: RoutePacket)
}

