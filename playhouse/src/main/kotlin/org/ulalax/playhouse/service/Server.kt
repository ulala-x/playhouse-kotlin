package org.ulalax.playhouse.service

interface Server {
    fun start()
    fun stop()
    fun awaitTermination()

}