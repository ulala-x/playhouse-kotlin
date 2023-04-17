package org.ulalax.playhouse.service.session

import io.netty.channel.Channel
import LOG
import org.ulalax.playhouse.communicator.*
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.protocol.Server.*
import org.ulalax.playhouse.communicator.message.ClientPacket
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.communicator.message.ReplyPacket

data class TargetAddress(val endpoint:String, val stageId: Long = 0)

class StageIndexGenerator
{
    private var byteValue: Byte = 0
    fun incrementByte():Byte {
        byteValue = ((byteValue + 1) and 0xff).toByte()
        return byteValue
    }
}

class SessionClient(
        serviceId: Short,
        private val sid: Int,
        private val channel: Channel,
        private val serviceInfoCenter: ServerInfoCenter,
        clientCommunicator: ClientCommunicator,
        urls: ArrayList<String>,
        reqCache: RequestCache,

) {

    private val sessionSender = XSessionSender(serviceId,clientCommunicator,reqCache)
    private val targetServiceCache = TargetServiceCache(serviceInfoCenter)

    var isAuthenticated = false
    private val signInURIs = HashSet<String>()
    private var accountId:Long = 0L
    private var playEndpoints = hashMapOf<Byte,TargetAddress>()
    private var authenticateServiceId:Short = 0.toShort()
    private var authServerEndpoint:String = ""
    private val stageIndexGenerator = StageIndexGenerator()

    init {
        signInURIs.addAll(urls)
    }
    private fun authenticate(serviceId: Short,apiEndpoint:String, accountId:Long){
        this.accountId = accountId
        isAuthenticated = true
        authenticateServiceId = serviceId
        this.authServerEndpoint = apiEndpoint
    }

    fun disconnect() {
        if(isAuthenticated){
            val serverInfo = findSuitableServer(authenticateServiceId,authServerEndpoint)
            val disconnectPacket = Packet(DisconnectNoticeMsg.newBuilder().setAccountId(accountId).build())
            sessionSender.sendToBaseApi(serverInfo.bindEndpoint(),disconnectPacket)
            playEndpoints.forEach{ (_, targetId) ->
                val targetServer = serviceInfoCenter.findServer(targetId.endpoint)
                sessionSender.sendToBaseStage(targetServer.bindEndpoint,targetId.stageId,accountId,disconnectPacket)
            }
        }
    }

    // from client
    fun onReceive(clientPacket: ClientPacket) {
        val serviceId = clientPacket.serviceId()
        val msgName = clientPacket.msgId()
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

    private fun findSuitableServer(serviceId: Short, endpoint: String):ServerInfo{
        var serverInfo:ServerInfo
        serverInfo = serviceInfoCenter.findServer(endpoint)
        if(serverInfo.state() != ServerState.RUNNING){
            serverInfo = serviceInfoCenter.findServerByAccountId(serviceId,accountId)
        }
        return serverInfo
    }


    private fun relayTo(serviceId: Short, clientPacket: ClientPacket) {
        
        val type = targetServiceCache.findTypeBy(serviceId)

        var serverInfo:ServerInfo

        when(type){
            ServiceType.API -> {
                if(authServerEndpoint.isEmpty()){
                    serverInfo = serviceInfoCenter.findRoundRobinServer(serviceId)
                }else{
                    serverInfo = findSuitableServer(serviceId,authServerEndpoint)
                }
                
                sessionSender.relayToApi(serverInfo.bindEndpoint(),sid,accountId,clientPacket)
            }
            ServiceType.Play ->{

                val targetId = playEndpoints[clientPacket.header.stageIndex]
                if(targetId == null){
                    LOG.error("Target Stage is not exist - service type:$type, msgId:${clientPacket.msgId()}",this)
                }else{
                    serverInfo = serviceInfoCenter.findServer(targetId.endpoint)
                    sessionSender.relayToStage(serverInfo.bindEndpoint(),targetId.stageId,sid,accountId,clientPacket)
                }
            }
            else ->{
                    LOG.error("Invalid Service Type request - service type:$type, msgId:${clientPacket.msgId()}",this)
            }
        }
    }

    // from backend server
    fun onReceive(packet: RoutePacket) {
        val msgName = packet.msgId()
        val isBase = packet.isBase()

        if(isBase){
            when(msgName){
                AuthenticateMsg.getDescriptor().index -> {
                    val authenticateMsg = AuthenticateMsg.parseFrom(packet.data())
                    val apiEndpoint = packet.routeHeader.from
                    authenticate(authenticateMsg.serviceId.toShort(),apiEndpoint, authenticateMsg.accountId)
                    LOG.debug("authenticated - accountId:$accountId ,from:$apiEndpoint",this)
                }

                SessionCloseMsg.getDescriptor().index -> {
                    channel.disconnect()
                    LOG.debug("session close - accountId:$accountId ",this)
                }

                JoinStageInfoUpdateReq.getDescriptor().index ->{
                    val joinStageMsg = JoinStageInfoUpdateReq.parseFrom(packet.data())
                    val playEndpoint =joinStageMsg.playEndpoint
                    val stageId = joinStageMsg.stageId
                    val stageIndex = updateStageInfo(playEndpoint,stageId)

                    sessionSender.reply(
                        ReplyPacket(JoinStageInfoUpdateRes.newBuilder().setStageIdx(stageIndex.toInt()).build()))

                    LOG.debug("stage info updated - accountId:$accountId, endpoint:$playEndpoint, stageId:$stageId $",this)
                }
                LeaveStageMsg.getDescriptor().index->{
                    val stageId = LeaveStageMsg.parseFrom(packet.data()).stageId
                    clearRoomInfo(stageId)
                    LOG.debug("stage info clear -  accountId:$accountId, stageId:$stageId" ,this)
                }
                else ->{
                    LOG.error("Invalid Packet $msgName",this)
                }
            }
        }else{
            sendToClient(packet.toClientPacket())
        }

    }

    private fun updateStageInfo(playEndpoint: String, stageId: Long): Byte {

        var stageIndex:Byte? = null
        this.playEndpoints.forEach{action->
            if(action.value.stageId == stageId){
                stageIndex = action.key
            }
        }
        if(stageIndex ==null){
            stageIndex = stageIndexGenerator.incrementByte()
        }

        this.playEndpoints[stageIndex!!] = TargetAddress(playEndpoint,stageId)

        return stageIndex!!
    }
    private fun clearRoomInfo(stageId: Long){
        var stageIndex:Byte? = null
        this.playEndpoints.forEach{action->
            if(action.value.stageId == stageId){
                stageIndex = action.key
            }
        }

        stageIndex?.apply {
            playEndpoints.remove(stageIndex)
        }
    }

    private fun sendToClient(clientPacket: ClientPacket){
        channel.writeAndFlush(clientPacket)
    }

}
