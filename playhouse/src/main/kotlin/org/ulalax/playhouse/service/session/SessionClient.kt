package org.ulalax.playhouse.service.session

import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.protocol.ClientPacket
import org.ulalax.playhouse.protocol.Packet
import org.ulalax.playhouse.service.RequestCache
import io.netty.channel.Channel
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import org.apache.logging.log4j.kotlin.logger
import org.ulalax.playhouse.protocol.Server.*
import org.ulalax.playhouse.communicator.CommunicateClient
import org.ulalax.playhouse.communicator.ServerInfoCenter
import org.ulalax.playhouse.communicator.ServiceType

class SessionClient(
    serviceId: String,
    private val sid: Int,
    private val channel: Channel,
    serviceInfoCenter: ServerInfoCenter,
    communicateClient: CommunicateClient,
    urls: ArrayList<String>,
    reqCache: RequestCache,
) {

    private val log = logger()

    private val sessionSender = SessionSenderImpl(serviceId,communicateClient,reqCache)
    private val targetServiceCache = TargetServiceCache(serviceInfoCenter)

    var isAuthenticated = false
    private val signInURIs = HashSet<String>()
    private val sessionData = HashMap<String,String>()
    private var accountId = 0L
    private var stageId:Long = 0L
    private var playEndpoint:String = ""
    private var authenticateServiceId = ""

    init {
        signInURIs.addAll(urls)
    }
    private fun authenticate(serviceId: String,accountId:Long,sessionInfo:String){
        this.accountId = accountId
        updateSessionInfo(serviceId,sessionInfo)
        isAuthenticated = true
        authenticateServiceId = serviceId
    }
    private fun updateSessionInfo(serviceId: String,sessionInfo:String) {
        sessionData[serviceId] = sessionInfo
    }

    fun disconnect() {
        if(isAuthenticated){

            targetServiceCache.getTargetedServers().forEach { serverInfo ->

                val discconectPacket = Packet(DisconnectNoticeMsg.newBuilder().setAccountId(accountId).build())
                when(serverInfo.serviceType){
                    ServiceType.API ->{
                        val sessionInfo = getSessionInfo(serverInfo.serviceId)
                        sessionSender.sendToBaseApi(serverInfo.bindEndpoint,sessionInfo,discconectPacket)
                    }
                    ServiceType.Play ->{
                        sessionSender.sendToBaseStage(serverInfo.bindEndpoint,stageId,accountId,discconectPacket)
                    }
                    else -> {
                        log.error("has invalid type session data : ${serverInfo.serviceType}")
                    }
                }

            }
        }

    }

    // from client
    fun onReceive(clientPacket: ClientPacket) {
        val serviceId = clientPacket.serviceId()
        val msgName = clientPacket.msgName()
        if(isAuthenticated){
            relayTo(serviceId, clientPacket)
        }else{

            val uri = "${serviceId}:${msgName}"
            if(signInURIs.contains(uri)){
                relayTo(serviceId, clientPacket)
            }else{
                log.warn("client is not authenticated :${msgName}")
                channel.disconnect()
            }
        }
    }

    private fun relayTo(serviceId: String, clientPacket: ClientPacket) {
        val sessionInfo = getSessionInfo(serviceId)
        val serverInfo = targetServiceCache.findServer(serviceId)
        val endpoint = serverInfo.bindEndpoint
        val type = serverInfo.serviceType
        val msgSeq = clientPacket.header.msgSeq


        when(type){
            ServiceType.API -> {
                sessionSender.relayToApi(endpoint,sid,sessionInfo,clientPacket.toPacket(),msgSeq)
            }
            ServiceType.Play ->{
                sessionSender.relayToRoom(endpoint,stageId,sid,accountId,sessionInfo,clientPacket.toPacket(),msgSeq)
            }
            else ->{
                    log.error("Invalid Serive Type request $type,${clientPacket.msgName()}")
            }
        }

        log.debug("session relayTo $type:${endpoint}, sessionInfo:$sessionInfo, msgName:${clientPacket.msgName()}")
    }

    fun getSessionInfo(serviceId: String):String  {
         return sessionData[serviceId] ?: ""
    }

    // from backend server
    fun onReceive(packet: RoutePacket) {
        val msgName = packet.msgName()
        val isBase = packet.isBase()
//        val serviceId = packet.serviceId()
//        val isBackend = packet.isBackend()

//        if(isBackend) {
//            log.error("session do not process backend packet: $msgName")
//            return
//        }

        if(isBase){
            when(msgName){
                AuthenticateMsg.getDescriptor().name -> {
                    val authenticateMsg = AuthenticateMsg.parseFrom(packet.buffer())
                    authenticate(authenticateMsg.serviceId,authenticateMsg.accountId,authenticateMsg.sessionInfo)
                    log.debug("$accountId is authenticated")
                }
                UpdateSessionInfoMsg.getDescriptor().name ->{
                    val updatedSessionInfo = UpdateSessionInfoMsg.parseFrom(packet.buffer())
                    updateSessionInfo(updatedSessionInfo.serviceId,updatedSessionInfo.sessionInfo)
                    log.debug("sessionInfo of $accountId is updated with $updatedSessionInfo")
                }
                SessionCloseMsg.getDescriptor().name -> {
                    channel.disconnect()
                    log.debug("$accountId is required to session close")
                }
                JoinStageMsg.getDescriptor().name ->{
                    val joinStageMsg = JoinStageMsg.parseFrom(packet.buffer())
                    val playEndpoint =joinStageMsg.playEndpoint
                    val stageId = joinStageMsg.stageId
                    updateRoomInfo(playEndpoint,stageId)
                    log.debug("$accountId is roomInfo updated:$playEndpoint,$stageId $")
                }
                LeaveStageMsg.getDescriptor().name->{
                    clearRoomInfo()
                    log.debug("$accountId is roomInfo clear:$playEndpoint,$stageId $")
                }
                else ->{
                    log.error("Invalid Packet $msgName")
                }
            }
        }else{
            sendToClient(packet.toClientPacket())
        }

    }

    private fun updateRoomInfo(playEndpoint: String, stageId: Long) {
        this.playEndpoint = playEndpoint
        this.stageId = stageId
    }
    private fun clearRoomInfo(){
        this.playEndpoint=""
        this.stageId=0
    }

    fun sendToClient(clientPacket: ClientPacket){
        //channel.writeAndFlush(BinaryWebSocketFrame(ByteBufferAllocator.getBuf(clientPacket.toMsg())))
//        clientPacket.use {
            channel.writeAndFlush(BinaryWebSocketFrame(clientPacket.toByteBuf()))
//        }
    }



}
