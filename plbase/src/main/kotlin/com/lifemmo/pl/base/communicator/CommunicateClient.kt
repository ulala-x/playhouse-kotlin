package com.lifemmo.pl.base.communicator;

import com.lifemmo.pl.base.communicator.message.RoutePacket


interface CommunicateClient {
    fun connect(endpoint:String):Unit
    fun send(endpoint:String, routePacket: RoutePacket):Unit
    fun communicate():Unit
    fun disconnect(endpoint: String)
}


