package org.ulalax.playhouse.base.service

import org.ulalax.playhouse.base.communicator.ServerInfo

interface Server {
    fun start()
    fun stop()
    fun awaitTermination()

}