package com.lifemmo.pl.base.service.room

import com.lifemmo.pl.base.protocol.Packet
import com.lifemmo.pl.base.protocol.ReplyPacket
import kotlinx.coroutines.CompletableDeferred

interface UserSender {
    fun accountId():Long
    fun sessionEndpoint():String
    fun apiEndpoint():String
    fun sid():Int
    fun leaveRoom()

    fun sendToClient(packet: Packet)

    fun sendToApi(sessionInfo: String,packet: Packet)
    suspend fun requestToApi(sessionInfo: String, packet: Packet): ReplyPacket
    fun deferredToApi(sessionInfo: String, packet: Packet): CompletableDeferred<ReplyPacket>




}