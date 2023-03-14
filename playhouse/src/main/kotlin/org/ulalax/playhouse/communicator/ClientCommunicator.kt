package org.ulalax.playhouse.communicator;

import org.ulalax.playhouse.communicator.message.RoutePacket
interface ClientCommunicator {
    fun connect(endpoint:String):Unit
    fun send(endpoint:String, routePacket: RoutePacket):Unit
    fun communicate():Unit
    fun disconnect(endpoint: String)
    fun stop()
}


