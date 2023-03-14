package org.ulalax.playhouse.communicator.socket

import org.ulalax.playhouse.communicator.message.RoutePacket

interface PlaySocket : AutoCloseable {
    val id:String
    fun bind()
    fun send(target: String, routePacket: RoutePacket)
    fun connect(target: String)
    fun receive(): RoutePacket?
    fun disconnect(endpoint: String)
}