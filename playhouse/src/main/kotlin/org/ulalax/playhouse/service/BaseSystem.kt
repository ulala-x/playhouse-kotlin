package org.ulalax.playhouse.service

import kotlinx.coroutines.runBlocking
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.apache.commons.lang3.exception.ExceptionUtils
import LOG
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

    private val START = -100;
    private val PAUSE = -101;
    private val RESUME = -102;
    private val STOP = -103;

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
                            when (routePacket.msgId()) {
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
                                    LOG.error("Invalid baseSystem packet ${routePacket.msgId()}",this)
                                }
                            }
                        } else {
                            baseSender.setCurrentPacketHeader(routePacket.routeHeader)
                            serverSystem.onDispatch(Packet(routePacket.msgId(), routePacket.movePayload()))
                        }
                    }catch (e:Exception){
                        LOG.error(ExceptionUtils.getStackTrace(e),this)
                        baseSender.errorReply(routePacket.routeHeader, BaseErrorCode.SYSTEM_ERROR_VALUE.toShort())
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