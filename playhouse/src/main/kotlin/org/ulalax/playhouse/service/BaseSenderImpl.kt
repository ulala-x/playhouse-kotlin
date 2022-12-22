package org.ulalax.playhouse.service

import org.ulalax.playhouse.communicator.CommunicateClient
import org.ulalax.playhouse.communicator.message.RouteHeader
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.protocol.Packet
import org.ulalax.playhouse.protocol.ReplyCallback
import org.ulalax.playhouse.protocol.ReplyPacket
import kotlinx.coroutines.CompletableDeferred
import org.apache.logging.log4j.kotlin.logger
import org.ulalax.playhouse.protocol.Common.*
import org.ulalax.playhouse.protocol.Server.*
import java.util.concurrent.CompletableFuture

open class BaseSenderImpl(private val serviceId: String,
                          private val communicateClient: CommunicateClient,
                          private val reqCache : RequestCache
) : BaseSender {

    private val log = logger()

    protected var currentHeader: RouteHeader? = null

    fun setCurrentPacketHeader(currentHeader: RouteHeader) {
        //if(currentHeader.header.msgSeq > 0){
            this.currentHeader = currentHeader
        //}
    }
    fun clearCurrentPacketHeader(){
        this.currentHeader = null
    }

    override fun serviceId(): String {
        return this.serviceId
    }

    override fun reply(reply: ReplyPacket){
        this.currentHeader ?.run {
            val msgSeq = header.msgSeq
            if(msgSeq != 0 ){
                val sid = this.sid
                val from = this.from
                val routePacket = RoutePacket.replyOf(serviceId,msgSeq,reply).apply {
                    this.routeHeader.sid = sid
                }
                communicateClient.send(from,routePacket)
            }else{
                log.error("not exist request packet ${reply.msgName},${header.msgName} is not request packet")
            }

        } ?: log.error("not exist request packet ${reply.msgName}")
    }


    override fun sendToClient(sessionEndpoint: String,sid:Int,packet: Packet){
        val routePacket = RoutePacket.clientOf(serviceId,sid,packet)
        communicateClient.send(sessionEndpoint, routePacket)
    }

    fun sendToBaseSession(sessionEndpoint: String, sid: Int, packet: Packet){
        val routePacket = RoutePacket.sessionOf(sid,packet, isBase = true, isBackend = true)
        communicateClient.send(sessionEndpoint,routePacket)
    }

    // 현재 session 은 request 를 사용할일이 없음
    suspend fun requestToBaseSession(sessionEndpoint: String, sid: Int, packet: Packet): ReplyPacket {

        val seq = getSequence()
        val deferred = CompletableDeferred<ReplyPacket>()
        reqCache.put(seq, ReplyObject(deferred =  deferred))
        val routePacket = RoutePacket.sessionOf(sid,packet,isBase = true, isBackend = true).apply {
            this.setMsgSeq(seq)
        }
        communicateClient.send(sessionEndpoint, routePacket)

        return deferred.await()
    }

    private fun getSequence(): Int {
        return reqCache.getSequence()
    }
    override fun sendToApi(apiEndpoint:String,sessionInfo: String,packet: Packet){
        val routePacket = RoutePacket.apiOf(sessionInfo,packet, isBase = false, isBackend = true)
        communicateClient.send(apiEndpoint, routePacket)
    }
    fun relayToApi(apiEndpoint: String, sid: Int, sessionInfo: String, packet: Packet, msgSeq: Int){
        val routePacket = RoutePacket.apiOf(sessionInfo,packet, isBase = false, isBackend = false).apply {
            this.routeHeader.sid = sid
            this.routeHeader.header.msgSeq = msgSeq
        }
        communicateClient.send(apiEndpoint, routePacket)
    }
    fun sendToBaseApi(apiEndpoint:String,sessionInfo: String,packet: Packet){
        val routePacket = RoutePacket.apiOf(sessionInfo,packet, isBase = true, isBackend = true)
        communicateClient.send(apiEndpoint, routePacket)
    }

    override fun sendToStage(playEndpoint:String, stageId:Long, accountId:Long, packet: Packet){
        val routePacket = RoutePacket.stageOf(stageId,accountId,packet,false, isBackend = true)
        communicateClient.send(playEndpoint, routePacket)
    }
    fun sendToBaseStage(playEndpoint:String, stageId:Long, accountId:Long, packet: Packet){
        val routePacket = RoutePacket.stageOf(stageId,accountId,packet,true, isBackend = true)
        communicateClient.send(playEndpoint, routePacket)
    }

//    override fun callToApi(apiEndpoint:String, packet: Packet, sessionInfo: String, replyCallback: ReplyCallback){
//        val seq = getSequence()
//        reqCache.put(seq, ReplyObject(callback = replyCallback))
//        var routePacket = RoutePacket.apiOf(sessionInfo,packet, isBase = false, isBackend = true).apply {
//            setMsgSeq(seq)
//        }
//        communicateClient.send(apiEndpoint, routePacket)
//    }
//
//    override fun callToApi(apiEndpoint: String, sessionInfo: String, packet: Packet): ReplyPacket {
//        val seq = getSequence()
//        val future = CompletableFuture<ReplyPacket>()
//        reqCache.put(seq, ReplyObject(future =  future))
//        var routePacket = RoutePacket.apiOf(sessionInfo,packet, isBase = false, isBackend = true).apply {
//            setMsgSeq(seq)
//        }
//        communicateClient.send(apiEndpoint, routePacket)
//        return future.get()
//    }

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
        communicateClient.send(apiEndpoint, routePacket)
        return deferred
    }


//    override fun callToRoom(playEndpoint:String, stageId:Long, accountId:Long, packet: Packet, replyCallback: ReplyCallback){
//        val seq = getSequence()
//        reqCache.put(seq, ReplyObject(callback = replyCallback))
//        var routePacket = RoutePacket.stageOf(stageId,accountId,packet, isBase = false, isBackend = true).apply {
//            setMsgSeq(seq)
//        }
//        communicateClient.send(playEndpoint,routePacket )
//    }
//
//    override fun callToRoom(playEndpoint: String, stageId: Long, accountId: Long, packet: Packet): ReplyPacket {
//        val seq = getSequence()
//        val future = CompletableFuture<ReplyPacket>()
//        reqCache.put(seq, ReplyObject(future = future))
//        var routePacket = RoutePacket.stageOf(stageId,accountId,packet, isBase = false, isBackend = true).apply {
//            setMsgSeq(seq)
//        }
//        communicateClient.send(playEndpoint,routePacket )
//
//        return future.get()
//    }

    fun callToBaseRoom(playEndpoint: String, stageId: Long, accountId: Long, packet: Packet): ReplyPacket {
        val seq = getSequence()
        val future = CompletableFuture<ReplyPacket>()
        reqCache.put(seq, ReplyObject(future = future))
        var routePacket = RoutePacket.stageOf(stageId,accountId,packet, isBase = true, isBackend = true).apply {
            setMsgSeq(seq)
        }
        communicateClient.send(playEndpoint,routePacket )

        return future.get()
    }


    override fun asyncToRoom(
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
        communicateClient.send(playEndpoint,routePacket )
        return deferred
    }
    override suspend fun requestToRoom(
        playEndpoint: String,
        stageId: Long,
        accountId: Long,
        packet: Packet,
    ): ReplyPacket {
        return asyncToRoom(playEndpoint,stageId,accountId,packet).await()
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
        communicateClient.send(playEndpoint,routePacket )
        return deferred.await()
    }

    override fun sendToSystem(endpoint: String, packet: Packet) {
        communicateClient.send(endpoint, RoutePacket.systemOf(packet,false))
    }

    override suspend fun requestToSystem(endpoint: String, packet: Packet): ReplyPacket {
        val msgSeq = this.reqCache.getSequence()
        val routePacket = RoutePacket.systemOf(packet,false).apply { routeHeader.header.msgSeq = msgSeq }
        val deferred = CompletableDeferred<ReplyPacket>()
        this.reqCache.put(msgSeq, ReplyObject(deferred = deferred))
        communicateClient.send(endpoint,routePacket)
        return deferred.await()
    }


//
//    fun onReply(routePacket: RoutePacket) = try {
//        val header = routePacket.routeHeader.header
//
//        val replyObject = reqCache.getIfPresent(header.msgSeq)
//        if(replyObject!=null){
//            currentHeader = replyObject.routeHeader
//            replyObject.onReceive(routePacket)
//        }else{
//            log.error{"${header.msgName} is not reply packet or timeout"}
//        }
//        reqCache.invalidate(header.msgSeq)
//    }catch (e:Exception){
//        log.error(ExceptionUtils.getStackTrace(e))
//    }

    fun errorReply(routeHeader: RouteHeader, errorCode:Int) {
        val msgSeq = routeHeader.header.msgSeq
        val from = routeHeader.from
        if(msgSeq >0) {
          val reply =  RoutePacket.replyOf(this.serviceId, msgSeq, ReplyPacket(BaseErrorCode.SUCCESS.number)).apply {
              this.routeHeader.header.baseErrorCode = errorCode
          }
          communicateClient.send(from,reply)
        }
    }

    fun relayToRoom(
        playEndpoint: String,
        stageId: Long,
        sid:Int,
        accountId: Long,
        sessionInfo: String,
        packet: Packet,
        msgSeq: Int
    ) {
        val routePacket = RoutePacket.apiOf(sessionInfo,packet, isBase = false, isBackend = false).apply {
            this.routeHeader.stageId = stageId
            this.routeHeader.accountId = accountId
            this.routeHeader.header.msgSeq = msgSeq
            this.routeHeader.sid = sid
        }
        communicateClient.send(playEndpoint, routePacket)
    }

    ///////////

    override fun sessionClose(sessionEndpoint: String,sid:Int) {
        val message = SessionCloseMsg.newBuilder().build()
        sendToBaseSession(sessionEndpoint,sid, Packet(message))
    }


}