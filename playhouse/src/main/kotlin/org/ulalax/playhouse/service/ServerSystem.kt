package org.ulalax.playhouse.service

import org.ulalax.playhouse.communicator.message.Packet


interface ServerSystem {
    val systemPanel: SystemPanel
    val baseSender: CommonSender
    suspend fun onStart()
    suspend fun onDispatch(packet: Packet)
    suspend fun onStop()
    suspend fun onPause()
    suspend fun onResume()

}