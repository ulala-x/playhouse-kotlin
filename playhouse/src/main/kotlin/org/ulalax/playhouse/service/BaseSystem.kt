package org.ulalax.playhouse.service

import kotlinx.coroutines.runBlocking
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.apache.commons.lang3.exception.ExceptionUtils
import org.ulalax.playhouse.LOG
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.protocol.Common.*
import java.lang.Thread.sleep
import java.util.concurrent.ConcurrentLinkedQueue

class BaseSystem(private val serverSystem: ServerSystem,
                 private val baseSender: BaseSender
) {


    private val thread = Thread({ messingLoop() },"system:message-loop")
    private val msgQueue = ConcurrentLinkedQueue<RoutePacket>()
    private var running = true

    private val START = "start"
    private val PAUSE = "pause"
    private val RESUME = "resume"
    private val STOP = "stop"

    fun start(){
        msgQueue.add(RoutePacket.systemOf(Packet(START),isBase = true))
        thread.start()
    }

    fun onReceive(packet: RoutePacket) {
        msgQueue.add(packet)
    }

    fun stop(){
        msgQueue.add(RoutePacket.systemOf(Packet(STOP),isBase = true))
        running = false
    }

    private fun messingLoop()  = runBlocking {
        while(running){
            var routePacket = msgQueue.poll()
            while(routePacket!=null){
                routePacket.use {
                    try{
                        if (routePacket.isBase()) {
                            when (routePacket.msgName()) {
                                START -> {
                                    serverSystem.onStart()
                                }
                                PAUSE -> {
                                    serverSystem.onPause()
                                }
                                RESUME -> {
                                    serverSystem.onResume()
                                }
                                STOP -> {
                                    serverSystem.onStop()
                                }
                                else -> {
                                    LOG.error("Invalid baseSystem packet ${routePacket.msgName()}",this)
                                }
                            }
                        } else {
                            baseSender.setCurrentPacketHeader(routePacket.routeHeader)
                            serverSystem.onDispatch(Packet(routePacket.msgName(), routePacket.movePayload()))
                        }
                    }catch (e:Exception){
                        LOG.error(ExceptionUtils.getStackTrace(e),this)
                        baseSender.errorReply(routePacket.routeHeader, BaseErrorCode.SYSTEM_ERROR_VALUE)
                    }finally {
                        baseSender.clearCurrentPacketHeader()
                    }
                    routePacket = msgQueue.poll()
                }
            }
            sleep(10)
        }
    }

    fun pause() {
        msgQueue.add(RoutePacket.systemOf(Packet(PAUSE),isBase = true))
    }

    fun resume() {
        msgQueue.add(RoutePacket.systemOf(Packet(RESUME),isBase = true))
    }

}