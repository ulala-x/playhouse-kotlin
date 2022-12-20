package com.lifemmo.client

import com.lifemmo.client.netty.ClientNetwork
import com.lifemmo.pl.base.protocol.ClientPacket
import com.lifemmo.pl.base.protocol.Packet
import com.lifemmo.pl.base.protocol.ReplyCallback
import com.lifemmo.pl.base.protocol.ReplyPacket
import kotlinx.coroutines.CompletableDeferred
import java.net.URI

class Connector(private val reqTimeoutSec:Long, clientPacketListener: ClientPacketListener ) {

    private lateinit var clientNetwork: ClientNetwork
    private val requestCache = RequestCache(reqTimeoutSec)
    private val connectorListener = BasePacketListener(requestCache,clientPacketListener)

    fun connect(host:String,port:Int){
        val url = "ws://$host:$port/websocket"
        val uri = URI(url)
        clientNetwork = ClientNetwork(connectorListener)
        clientNetwork.init(uri)
        clientNetwork.connect(uri.host, port)
    }

    suspend fun deferredConnect(host:String, port:Int){
        val url = "ws://$host:$port/websocket"
        val uri = URI(url)
        clientNetwork = ClientNetwork(connectorListener)
        clientNetwork.init(uri)
        clientNetwork.deferredConnect(uri.host, port,reqTimeoutSec)
    }

    fun disconnect(){
        clientNetwork.disconnect()
    }

    fun isConnect(): Boolean {
        return clientNetwork.isConnect()
    }

    fun send(serviceId:String, packet: Packet){
        val clientPacket = ClientPacket.toServerOf(serviceId,packet)
        clientNetwork.send(clientPacket)
    }

    fun request(serviceId:String, packet: Packet, replyCallback: ReplyCallback){
        val seq = requestCache.getSequence()

        val clientPacket = ClientPacket.toServerOf(serviceId,packet).apply {
            this.setMsgSeq(seq)
        }
        clientNetwork.send(clientPacket)
        requestCache.put(seq,ReplyObject(callback = replyCallback))
    }


    suspend fun request(serviceId:String, packet: Packet): ReplyPacket {
        val seq = requestCache.getSequence()

        val clientPacket = ClientPacket.toServerOf(serviceId,packet).apply {
            this.setMsgSeq(seq)
        }
        clientNetwork.send(clientPacket)

        val deferred = CompletableDeferred<ReplyPacket>()
        requestCache.put(seq,ReplyObject(deferred = deferred))
        return deferred.await()
    }


}