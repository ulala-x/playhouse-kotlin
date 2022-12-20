package com.lifemmo.client

import com.lifemmo.pl.base.protocol.Packet


fun interface ClientPacketListener {
    fun onReceive(serviceId: String, packet: Packet)
}