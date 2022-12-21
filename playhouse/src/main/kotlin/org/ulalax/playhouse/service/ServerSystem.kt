package org.ulalax.playhouse.service

import org.ulalax.playhouse.protocol.Packet

interface ServerSystem {
    val baseSender: BaseSender
    val systemPanel: SystemPanel

    fun onStart()

    fun onDispatch(packet: Packet)
    fun onStop()
    fun onPause()
    fun onResume()

}