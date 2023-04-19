package org.ulalax.playhouse.client

import org.ulalax.playhouse.client.network.ClientNetwork
import kotlinx.coroutines.CompletableDeferred
import org.ulalax.playhouse.client.network.message.*
import java.net.URI


class Connector(private val reqTimeoutSec:Long,
                private val useWebsocket: Boolean=false,
                apiPacketListener: ApiPacketListener,
                stagePacketListener: StagePacketListener,
        ) {

    private lateinit var clientNetwork: ClientNetwork
    private val requestCache = RequestCache(reqTimeoutSec)
    private val connectorListener = BasePacketListener(requestCache,apiPacketListener,stagePacketListener)

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

    suspend fun connectAsync(host:String, port:Int){
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


    fun sendToApi(serviceId:Short, packet: Packet){
        sendToStage(serviceId,0,packet)
    }


    fun requestToApi(serviceId: Short, packet: Packet, replyCallback: ReplyCallback){
        requestToStage(serviceId,0,packet,replyCallback)
    }


    suspend fun requestToApi(serviceId: Short, packet: Packet): ReplyPacket {
       return requestToStage(serviceId,0,packet)
    }

    fun sendToStage(serviceId:Short, stageIndex:Int, packet: Packet){
        val clientPacket = ClientPacket.toServerOf(TargetId(serviceId,stageIndex),packet)
        clientNetwork.send(clientPacket)
    }


    fun requestToStage(serviceId: Short, stageIndex:Int, packet: Packet, replyCallback: ReplyCallback){
        val seq = requestCache.getSequence()

        val clientPacket = ClientPacket.toServerOf(TargetId(serviceId,stageIndex),packet).apply {
            this.setMsgSeq(seq.toShort() )
        }
        clientNetwork.send(clientPacket)
        requestCache.put(seq, ReplyObject(callback = replyCallback))
    }


    suspend fun requestToStage(serviceId: Short, stageIndex:Int, packet: Packet): ReplyPacket {
        val seq = requestCache.getSequence()

        val clientPacket = ClientPacket.toServerOf(TargetId(serviceId,stageIndex),packet).apply {
            this.setMsgSeq(seq)
        }
        clientNetwork.send(clientPacket)

        val deferred = CompletableDeferred<ReplyPacket>()
        requestCache.put(seq, ReplyObject(deferred = deferred))
        return deferred.await()
    }


}