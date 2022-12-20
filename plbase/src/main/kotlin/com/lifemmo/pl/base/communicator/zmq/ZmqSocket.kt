package com.lifemmo.pl.base.communicator.zmq

import com.lifemmo.pl.base.communicator.message.RoutePacket

interface ZmqSocket : AutoCloseable {
    val bindEndpoint:String
    fun bind()
    fun send(target: String, routePacket: RoutePacket)
    fun connect(target: String)
    fun receive(): RoutePacket?
    fun disconnect(endpoint: String)
}