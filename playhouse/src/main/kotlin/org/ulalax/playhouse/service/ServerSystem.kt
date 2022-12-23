package org.ulalax.playhouse.service

import org.ulalax.playhouse.protocol.Packet

interface ServerSystem {
    val systemPanel: SystemPanel
    val baseSender: BaseSender
    suspend fun onStart()
    suspend fun onDispatch(packet: Packet)
    suspend fun onStop()
    suspend fun onPause()
    suspend fun onResume()

}