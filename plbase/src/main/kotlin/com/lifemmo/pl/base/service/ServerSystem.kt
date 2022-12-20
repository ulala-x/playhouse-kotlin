package com.lifemmo.pl.base.service

import com.lifemmo.pl.base.protocol.Packet

interface ServerSystem {
    val baseSender:BaseSender
    val systemPanel:SystemPanel

    fun onStart()

    fun onDispatch(packet: Packet)
    fun onStop()
    fun onPause()
    fun onResume()

}