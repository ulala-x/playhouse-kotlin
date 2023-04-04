package org.ulalax.playhouse.service.session

import io.netty.channel.Channel
import LOG
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.protocol.Server.*
import org.ulalax.playhouse.communicator.ClientCommunicator
import org.ulalax.playhouse.communicator.RequestCache
import org.ulalax.playhouse.communicator.ServerInfoCenter
import org.ulalax.playhouse.communicator.ServiceType
import org.ulalax.playhouse.communicator.message.ClientPacket
import org.ulalax.playhouse.communicator.message.Packet

class SessionClient(
        serviceId: String,
        private val sid: Int,
        private val channel: Channel,
        serviceInfoCenter: ServerInfoCenter,
        clientCommunicator: ClientCommunicator,
        urls: ArrayList<String>,
        reqCache: RequestCache,

) {

    private val sessionSender = XSessionSender(serviceId,clientCommunicator,reqCache)
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
                        LOG.error("has invalid type session data : ${serverInfo.serviceType}",this)
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
                LOG.warn("client is not authenticated :${msgName}",this)
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
                sessionSender.relayToApi(endpoint,sid,sessionInfo,clientPacket,msgSeq)
            }
            ServiceType.Play ->{
                sessionSender.relayToRoom(endpoint,stageId,sid,accountId,sessionInfo,clientPacket,msgSeq)
            }
            else ->{
                    LOG.error("Invalid Serive Type request $type,${clientPacket.msgName()}",this)
            }
        }

        LOG.debug("session relayTo $type:${endpoint}, sessionInfo:$sessionInfo, msgName:${clientPacket.msgName()}",this)
    }

    fun getSessionInfo(serviceId: String):String  {
         return sessionData[serviceId] ?: ""
    }

    // from backend server
    fun onReceive(packet: RoutePacket) {
        val msgName = packet.getMsgName()
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
                    val authenticateMsg = AuthenticateMsg.parseFrom(packet.data())
                    authenticate(authenticateMsg.serviceId,authenticateMsg.accountId,authenticateMsg.sessionInfo)
                    LOG.debug("$accountId is authenticated",this)
                }
                UpdateSessionInfoMsg.getDescriptor().name ->{
                    val updatedSessionInfo = UpdateSessionInfoMsg.parseFrom(packet.data())
                    updateSessionInfo(updatedSessionInfo.serviceId,updatedSessionInfo.sessionInfo)
                    LOG.debug("sessionInfo of $accountId is updated with $updatedSessionInfo",this)
                }
                SessionCloseMsg.getDescriptor().name -> {
                    channel.disconnect()
                    LOG.debug("$accountId is required to session close",this)
                }
                JoinStageMsg.getDescriptor().name ->{
                    val joinStageMsg = JoinStageMsg.parseFrom(packet.data())
                    val playEndpoint =joinStageMsg.playEndpoint
                    val stageId = joinStageMsg.stageId
                    updateRoomInfo(playEndpoint,stageId)
                    LOG.debug("$accountId is roomInfo updated:$playEndpoint,$stageId $",this)
                }
                LeaveStageMsg.getDescriptor().name->{
                    clearRoomInfo()
                    LOG.debug("$accountId is roomInfo clear:$playEndpoint,$stageId $",this)
                }
                else ->{
                    LOG.error("Invalid Packet $msgName",this)
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

    private fun sendToClient(clientPacket: ClientPacket){
        channel.writeAndFlush(clientPacket)
    }

}
