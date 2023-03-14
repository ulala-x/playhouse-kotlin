package org.ulalax.playhouse.client

import org.ulalax.playhouse.client.network.ClientNetwork
import kotlinx.coroutines.CompletableDeferred
import org.ulalax.playhouse.client.network.message.ClientPacket
import org.ulalax.playhouse.client.network.message.Packet
import org.ulalax.playhouse.client.network.message.ReplyCallback
import org.ulalax.playhouse.client.network.message.ReplyPacket
import java.net.URI

class Connector(private val reqTimeoutSec:Long,private val useWebsocket: Boolean=false, clientPacketListener: ClientPacketListener ) {

    private lateinit var clientNetwork: ClientNetwork
    private val requestCache = RequestCache(reqTimeoutSec)
    private val connectorListener = BasePacketListener(requestCache,clientPacketListener)

    fun connect(host:String,port:Int){

        clientNetwork = ClientNetwork(connectorListener)

        if(useWebsocket){
            val url = "ws://$host:$port/websocket"
            val uri = URI(url)
            clientNetwork.init(uri)
            clientNetwork.connect(uri.host, port)
        }else{
            clientNetwork.init(null)
            clientNetwork.connect(host,port)
        }


    }

    suspend fun deferredConnect(host:String, port:Int){
        clientNetwork = ClientNetwork(connectorListener)

        if(useWebsocket){
            val url = "ws://$host:$port/websocket"
            val uri = URI(url)
            clientNetwork.init(uri)
            clientNetwork.deferredConnect(uri.host, port,reqTimeoutSec)
        }else{
            clientNetwork.init(null)
            clientNetwork.deferredConnect(host,port,reqTimeoutSec)
        }
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
        requestCache.put(seq, ReplyObject(callback = replyCallback))
    }


    suspend fun request(serviceId:String, packet: Packet): ReplyPacket {
        val seq = requestCache.getSequence()

        val clientPacket = ClientPacket.toServerOf(serviceId,packet).apply {
            this.setMsgSeq(seq)
        }
        clientNetwork.send(clientPacket)

        val deferred = CompletableDeferred<ReplyPacket>()
        requestCache.put(seq, ReplyObject(deferred = deferred))
        return deferred.await()
    }


}