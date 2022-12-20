package com.lifemmo.pl.base.communicator

import com.lifemmo.pl.base.communicator.message.RoutePacket
import org.apache.logging.log4j.kotlin.logger
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

open class PacketBucket {
    val log = logger()
    private val queue: MutableMap<String,Queue<RoutePacket>> = ConcurrentHashMap();

    fun add(target: String, routePacket: RoutePacket){
        var packets = queue[target]
        if(packets==null){
            packets = ConcurrentLinkedQueue()
            this.queue[target] = packets
        }
        packets.add(routePacket)
    }

    fun get(): MutableMap<String, Queue<RoutePacket>> {
        return queue
    }
}

class SendBucket : PacketBucket()
class ReceiveBucket : PacketBucket()