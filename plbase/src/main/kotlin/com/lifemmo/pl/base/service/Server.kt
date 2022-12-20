package com.lifemmo.pl.base.service

import com.lifemmo.pl.base.communicator.ServerInfo

interface Server {
    fun start()
    fun stop()
    fun awaitTermination()

}