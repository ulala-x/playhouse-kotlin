package org.ulalax.playhouse.client

import org.ulalax.playhouse.client.network.message.Packet

fun interface ApiPacketListener {
    fun onReceive(serviceId: Short,packet: Packet)
}

fun interface StagePacketListener {
    fun onReceive(serviceId: Short,stageIndex:Int,packet: Packet)
}