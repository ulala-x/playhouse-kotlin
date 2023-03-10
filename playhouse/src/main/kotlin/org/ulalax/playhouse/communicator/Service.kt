package org.ulalax.playhouse.communicator;

import org.ulalax.playhouse.communicator.message.RoutePacket

enum class ServiceType {
    SESSION,
    API,
    Play
}

interface IService {
    fun onStart()
    fun onReceive(routePacket: RoutePacket)
    fun onStop()
    fun weightPoint():Int
    fun serverState(): ServerState
    fun serviceType(): ServiceType
    fun serviceId():String
    fun pause()
    fun resume()
}
