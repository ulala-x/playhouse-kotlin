package com.lifemmo.pl.base.communicator

import com.lifemmo.pl.base.communicator.message.RoutePacket

interface CommunicateListener {
    fun onReceive(routePacket: RoutePacket)
}

