package org.ulalax.playhouse.service.play

import org.ulalax.playhouse.protocol.Packet
import org.ulalax.playhouse.protocol.ReplyPacket
import kotlinx.coroutines.CompletableDeferred

interface ActorSender {
    fun accountId():Long
    fun sessionEndpoint():String
    fun apiEndpoint():String
    fun sid():Int
    fun leaveStage()

    fun sendToClient(packet: Packet)

    fun sendToApi(sessionInfo: String,packet: Packet)
    suspend fun requestToApi(sessionInfo: String, packet: Packet): ReplyPacket
    fun asyncToApi(sessionInfo: String, packet: Packet): CompletableDeferred<ReplyPacket>




}