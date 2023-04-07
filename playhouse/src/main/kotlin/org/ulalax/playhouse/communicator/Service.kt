package org.ulalax.playhouse.communicator;

import org.ulalax.playhouse.communicator.message.RoutePacket

enum class ServiceType {
    SESSION,
    API,
    Play
}

interface Service {

    val serviceId:Short
    fun getWeightPoint():Int
    fun getServerState(): ServerState
    fun getServiceType(): ServiceType

    fun onStart()
    fun onReceive(routePacket: RoutePacket)
    fun onStop()

    fun pause()
    fun resume()
}
