package org.ulalax.playhouse.service

import org.ulalax.playhouse.protocol.Packet

interface ServerSystem {
    val systemPanel: SystemPanel
    val baseSender: BaseSender
    fun onStart()
    fun onDispatch(packet: Packet)
    fun onStop()
    fun onPause()
    fun onResume()

}