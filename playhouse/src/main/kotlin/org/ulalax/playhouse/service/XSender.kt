package org.ulalax.playhouse.service

import org.ulalax.playhouse.communicator.ClientCommunicator
import kotlinx.coroutines.CompletableDeferred
import LOG
import org.ulalax.playhouse.communicator.ReplyObject
import org.ulalax.playhouse.communicator.RequestCache
import org.ulalax.playhouse.communicator.message.*
import org.ulalax.playhouse.protocol.Server.*
import java.util.concurrent.CompletableFuture

open class XSender(private val serviceId: Short,
                   private val clientCommunicator: ClientCommunicator,
                   private val reqCache : RequestCache
) : Sender {

    protected var currentHeader: RouteHeader? = null

    fun setCurrentPacketHeader(currentHeader: RouteHeader) {
        //if(currentHeader.header.msgSeq > 0){
            this.currentHeader = currentHeader
        //}
    }
    fun clearCurrentPacketHeader(){
        this.currentHeader = null
    }

    override fun serviceId(): Short {
        return this.serviceId
    }

    override fun reply(reply: ReplyPacket){
        this.currentHeader ?.run {
            val msgSeq = header.msgSeq
            if(msgSeq != 0.toShort() ){
                val sid = this.sid
                val from = this.from
                val routePacket = RoutePacket.replyOf(serviceId,msgSeq,reply).apply {
                    this.routeHeader.sid = sid
                    this.routeHeader.forClient = forClient
                }
                clientCommunicator.send(from,routePacket)
            }else{
                LOG.error("not exist request packet ${reply.msgId},${header.msgId} is not request packet",this)
            }

        } ?: LOG.error("not exist request packet ${reply.msgId}",this)
    }


    override fun sendToClient(sessionEndpoint: String,sid:Int,packet: Packet){
        val routePacket = RoutePacket.clientOf(serviceId,sid,packet)
        clientCommunicator.send(sessionEndpoint, routePacket)
    }

    fun sendToBaseSession(sessionEndpoint: String, sid: Int, packet: Packet){
        val routePacket = RoutePacket.sessionOf(sid,packet, isBase = true, isBackend = true)
        clientCommunicator.send(sessionEndpoint,routePacket)
    }

    // 현재 session 은 request 를 사용할일이 없음
    suspend fun requestToBaseSession(sessionEndpoint: String, sid: Int, packet: Packet): ReplyPacket {

        val seq = getSequence()
        val deferred = CompletableDeferred<ReplyPacket>()
        reqCache.put(seq, ReplyObject(deferred =  deferred))
        val routePacket = RoutePacket.sessionOf(sid,packet,isBase = true, isBackend = true).apply {
            this.setMsgSeq(seq)
        }
        clientCommunicator.send(sessionEndpoint, routePacket)

        return deferred.await()
    }

    private fun getSequence(): Short {
        return reqCache.getSequence()
    }
    override fun sendToApi(apiEndpoint:String,sessionInfo: String,packet: Packet){
        val routePacket = RoutePacket.apiOf(sessionInfo,packet, isBase = false, isBackend = true)
        clientCommunicator.send(apiEndpoint, routePacket)
    }
    fun relayToApi(apiEndpoint: String, sid: Int, sessionInfo: String, packet: Packet, msgSeq: Short){
        val routePacket = RoutePacket.apiOf(sessionInfo,packet, isBase = false, isBackend = false).apply {
            this.routeHeader.sid = sid
            this.routeHeader.header.msgSeq = msgSeq
        }
        clientCommunicator.send(apiEndpoint, routePacket)
    }
    fun sendToBaseApi(apiEndpoint:String,sessionInfo: String,packet: Packet){
        val routePacket = RoutePacket.apiOf(sessionInfo,packet, isBase = true, isBackend = true)
        clientCommunicator.send(apiEndpoint, routePacket)
    }

    override fun sendToStage(playEndpoint:String, stageId:Long, accountId:Long, packet: Packet){
        val routePacket = RoutePacket.stageOf(stageId,accountId,packet,false, isBackend = true)
        clientCommunicator.send(playEndpoint, routePacket)
    }
    fun sendToBaseStage(playEndpoint:String, stageId:Long, accountId:Long, packet: Packet){
        val routePacket = RoutePacket.stageOf(stageId,accountId,packet,true, isBackend = true)
        clientCommunicator.send(playEndpoint, routePacket)
    }

    override fun requestToApi(apiEndpoint:String, packet: Packet, sessionInfo: String, replyCallback: ReplyCallback){
        val seq = getSequence()
        reqCache.put(seq, ReplyObject(callback = replyCallback))
        var routePacket = RoutePacket.apiOf(sessionInfo,packet, isBase = false, isBackend = true).apply {
            setMsgSeq(seq)
        }
        clientCommunicator.send(apiEndpoint, routePacket)
    }


    override suspend fun requestToApi(apiEndpoint: String, sessionInfo: String, packet: Packet): ReplyPacket {
        return asyncToApi(apiEndpoint,sessionInfo,packet).await()
    }

    override fun asyncToApi(
        apiEndpoint: String,
        sessionInfo: String,
        packet: Packet,
    ): CompletableDeferred<ReplyPacket> {
        val seq = getSequence()
        val deferred = CompletableDeferred<ReplyPacket>()
        reqCache.put(seq, ReplyObject(deferred =  deferred))
        var routePacket = RoutePacket.apiOf(sessionInfo,packet, isBase = false, isBackend = true).apply {
            setMsgSeq(seq)
        }
        clientCommunicator.send(apiEndpoint, routePacket)
        return deferred
    }


    override fun requestToRoom(playEndpoint:String, stageId:Long, accountId:Long, packet: Packet, replyCallback: ReplyCallback){
        val seq = getSequence()
        reqCache.put(seq, ReplyObject(callback = replyCallback))
        var routePacket = RoutePacket.stageOf(stageId,accountId,packet, isBase = false, isBackend = true).apply {
            setMsgSeq(seq)
        }
        clientCommunicator.send(playEndpoint,routePacket )
    }

    fun callToBaseRoom(playEndpoint: String, stageId: Long, accountId: Long, packet: Packet): ReplyPacket {
        val seq = getSequence()
        val future = CompletableFuture<ReplyPacket>()
        reqCache.put(seq, ReplyObject(future = future))
        var routePacket = RoutePacket.stageOf(stageId,accountId,packet, isBase = true, isBackend = true).apply {
            setMsgSeq(seq)
        }
        clientCommunicator.send(playEndpoint,routePacket )

        return future.get()
    }


    override fun asyncToStage(
        playEndpoint: String,
        stageId: Long,
        accountId: Long,
        packet: Packet,
    ): CompletableDeferred<ReplyPacket> {
        val seq = getSequence()
        val deferred = CompletableDeferred<ReplyPacket>()
        reqCache.put(seq, ReplyObject(deferred = deferred))
        var routePacket = RoutePacket.stageOf(stageId,accountId,packet, isBase = false, isBackend = true).apply {
            setMsgSeq(seq)
        }
        clientCommunicator.send(playEndpoint,routePacket )
        return deferred
    }
    override suspend fun requestToStage(
        playEndpoint: String,
        stageId: Long,
        accountId: Long,
        packet: Packet,
    ): ReplyPacket {
        return asyncToStage(playEndpoint,stageId,accountId,packet).await()
    }

    suspend fun requestToBaseRoom(
        playEndpoint: String,
        stageId: Long,
        accountId: Long,
        packet: Packet,
    ): ReplyPacket {
        val seq = getSequence()
        val deferred = CompletableDeferred<ReplyPacket>()
        reqCache.put(seq, ReplyObject(deferred = deferred))
        var routePacket = RoutePacket.stageOf(stageId,accountId,packet, isBase = true, isBackend = true).apply {
            setMsgSeq(seq)
        }
        clientCommunicator.send(playEndpoint,routePacket )
        return deferred.await()
    }

    override fun sendToSystem(endpoint: String, packet: Packet) {
        clientCommunicator.send(endpoint, RoutePacket.systemOf(packet,false))
    }

    override suspend fun requestToSystem(endpoint: String, packet: Packet): ReplyPacket {
        val msgSeq = this.reqCache.getSequence()
        val routePacket = RoutePacket.systemOf(packet,false).apply { routeHeader.header.msgSeq = msgSeq }
        val deferred = CompletableDeferred<ReplyPacket>()
        this.reqCache.put(msgSeq, ReplyObject(deferred = deferred))
        clientCommunicator.send(endpoint,routePacket)
        return deferred.await()
    }



    fun errorReply(routeHeader: RouteHeader, errorCode: Short) {
        val msgSeq = routeHeader.header.msgSeq
        val from = routeHeader.from
        if(msgSeq >0) {
          val reply =  RoutePacket.replyOf(this.serviceId, msgSeq, ReplyPacket(errorCode = errorCode))
          clientCommunicator.send(from,reply)
        }
    }

    fun relayToRoom(
        playEndpoint: String,
        stageId: Long,
        sid:Int,
        accountId: Long,
        sessionInfo: String,
        packet: Packet,
        msgSeq: Short
    ) {
        val routePacket = RoutePacket.apiOf(sessionInfo,packet, isBase = false, isBackend = false).apply {
            this.routeHeader.stageId = stageId
            this.routeHeader.accountId = accountId
            this.routeHeader.header.msgSeq = msgSeq
            this.routeHeader.sid = sid
        }
        clientCommunicator.send(playEndpoint, routePacket)
    }

    ///////////

    override fun sessionClose(sessionEndpoint: String,sid:Int) {
        val message = SessionCloseMsg.newBuilder().build()
        sendToBaseSession(sessionEndpoint,sid, Packet(message))
    }


}