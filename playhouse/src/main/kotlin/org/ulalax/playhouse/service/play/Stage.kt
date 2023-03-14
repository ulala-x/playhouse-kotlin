package org.ulalax.playhouse.service.play

import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.communicator.message.ReplyPacket
import org.ulalax.playhouse.service.StageSender

interface Stage<A: Actor> {
    val stageSender: StageSender
    suspend fun onCreate(packet: Packet): ReplyPacket
    suspend fun onJoinStage(actor: A, packet: Packet): ReplyPacket
    suspend fun onDispatch(actor: A, packet: Packet)
    suspend fun onDisconnect(actor: A)
    suspend fun onPostCreate()
    suspend fun onPostJoinStage(actor: A)
}

