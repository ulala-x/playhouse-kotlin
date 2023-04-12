package org.ulalax.playhouse.service.play

import kotlinx.coroutines.CompletableDeferred
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.communicator.message.ReplyPacket

interface ActorSender {
    fun accountId():Long
    fun sessionEndpoint():String
    fun apiEndpoint():String
    fun sid():Int
    fun leaveStage()

    fun sendToClient(packet: Packet)

    fun sendToApi(packet: Packet)
    suspend fun requestToApi(packet: Packet): ReplyPacket
    fun asyncToApi(packet: Packet): CompletableDeferred<ReplyPacket>


}