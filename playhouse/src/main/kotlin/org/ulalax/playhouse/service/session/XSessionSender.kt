package org.ulalax.playhouse.service.session

import org.ulalax.playhouse.communicator.ClientCommunicator
import org.ulalax.playhouse.communicator.RequestCache
import org.ulalax.playhouse.communicator.message.ClientPacket
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.service.XSender
import org.ulalax.playhouse.service.SessionSender


class XSessionSender(serviceId:Short,private val clientCommunicator: ClientCommunicator, reqCache: RequestCache) :
    XSender(serviceId,clientCommunicator,reqCache), SessionSender {

    fun relayToRoom(
            playEndpoint: String,
            stageId: Long,
            sid:Int,
            accountId: Long,
            packet: ClientPacket,
            msgSeq: Short
    ) {
        val routePacket = RoutePacket.apiOf(packet.toPacket(), isBase = false, isBackend = false).apply {
            this.routeHeader.stageId = stageId
            this.routeHeader.accountId = accountId
            this.routeHeader.header.msgSeq = msgSeq
            this.routeHeader.sid = sid
            this.routeHeader.forClient = true
        }
        clientCommunicator.send(playEndpoint, routePacket)
    }

    fun relayToApi(apiEndpoint: String, sid: Int,accountId: Long, packet: ClientPacket, msgSeq: Short){
        val routePacket = RoutePacket.apiOf(packet.toPacket(), isBase = false, isBackend = false).apply {
            this.routeHeader.sid = sid
            this.routeHeader.header.msgSeq = msgSeq
            this.routeHeader.forClient = true
            this.routeHeader.accountId = accountId
        }
        clientCommunicator.send(apiEndpoint, routePacket)
    }


}