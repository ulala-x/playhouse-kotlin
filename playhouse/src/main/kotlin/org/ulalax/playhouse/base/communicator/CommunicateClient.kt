package org.ulalax.playhouse.base.communicator;

import org.ulalax.playhouse.base.communicator.message.RoutePacket


interface CommunicateClient {
    fun connect(endpoint:String):Unit
    fun send(endpoint:String, routePacket: RoutePacket):Unit
    fun communicate():Unit
    fun disconnect(endpoint: String)
}


