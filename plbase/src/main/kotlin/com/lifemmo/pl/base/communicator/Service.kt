package com.lifemmo.pl.base.communicator;

import com.lifemmo.pl.base.Plbase
import com.lifemmo.pl.base.communicator.message.RoutePacket
import org.apache.logging.log4j.kotlin.logger

enum class ServiceType {
    SESSION,
    API,
    ROOM
}

interface Service {
    fun onStart()
    fun onReceive(routePacket: RoutePacket)
    fun onStop()
    fun weightPoint():Int
    fun serverState(): ServerInfo.ServerState
    fun serviceType():ServiceType
    fun serviceId():String
    fun pause()
    fun resume()
}

class DefaultService : Service {
    private val log = logger()
    override fun onStart() {
        log.info("DefaultService onStart")
    }

    override fun onReceive(routePacket: RoutePacket) {
        log.info("DefaultService onReceive:${routePacket.msgName()}")
    }

    override fun onStop() {
        log.info("DefaultService onStop")
    }

    override fun weightPoint(): Int {
        return 0
    }

    override fun serverState(): ServerInfo.ServerState {
        return ServerInfo.ServerState.RUNNING
    }

    override fun serviceType(): ServiceType {
        return ServiceType.API
    }

    override fun serviceId(): String {
        return "default"
    }

    override fun pause() {

    }

    override fun resume() {

    }



}
