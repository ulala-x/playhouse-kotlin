package org.ulalax.playhouse.service.play

interface Actor {
    val actorSender: ActorSender
    fun onCreate()
    fun onDestroy()
}