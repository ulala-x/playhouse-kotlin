package org.ulalax.playhouse.service.session

import io.netty.channel.Channel
import LOG
import org.apache.commons.lang3.exception.ExceptionUtils
import org.ulalax.playhouse.communicator.*
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.protocol.Server.*
import org.ulalax.playhouse.communicator.message.ClientPacket
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.communicator.message.ReplyPacket
import org.ulalax.playhouse.protocol.Common
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

data class TargetAddress(val endpoint:String, val stageId: Long = 0)

class StageIndexGenerator
{
    private var byteValue: UByte = 0u
    fun incrementByte():UByte {
        byteValue = ((byteValue + 1u) and 0xffu).toUByte()
        if(byteValue == 0.toUByte()){
            byteValue = incrementByte()
        }
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
    private var playEndpoints = hashMapOf<Int,TargetAddress>()
    private var authenticateServiceId:Short = 0.toShort()
    private var authServerEndpoint:String = ""
    private val stageIndexGenerator = StageIndexGenerator()
    private val msgQueue = ConcurrentLinkedQueue<RoutePacket>()
    private var isUsing = AtomicBoolean(false)

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
            val disconnectPacket = Packet(DisconnectNoticeMsg.newBuilder().build())
            sessionSender.sendToBaseApi(serverInfo.bindEndpoint(),accountId,disconnectPacket)
            playEndpoints.forEach{ (_, targetId) ->
                val targetServer = serviceInfoCenter.findServer(targetId.endpoint)
                sessionSender.sendToBaseStage(targetServer.bindEndpoint,targetId.stageId,accountId,disconnectPacket)
            }
        }
    }

    // from client
    fun dispatch(clientPacket: ClientPacket) {
        val serviceId = clientPacket.serviceId
        val msgName = clientPacket.msgId
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

                val stageIndex = clientPacket.header.stageIndex.toInt()
                val targetId = playEndpoints[stageIndex]
                if(targetId == null){
                    LOG.error("Target Stage is not exist - stageIndex:$stageIndex, msgId:${clientPacket.msgId}",this)
                }else{
                    serverInfo = serviceInfoCenter.findServer(targetId.endpoint)
                    sessionSender.relayToStage(serverInfo.bindEndpoint(),targetId.stageId,sid,accountId,clientPacket)
                }
            }
            else ->{
                    LOG.error("Invalid Service Type request - service type:$type, msgId:${clientPacket.msgId}",this)
            }
        }
    }

    fun send(routePacket: RoutePacket)  {
        msgQueue.add(routePacket)
        if(isUsing.compareAndSet(false,true)){
            while(isUsing.get()){
                val item = msgQueue.poll()
                if(item!=null) {
                    try {
                        item.use {
                            sessionSender.setCurrentPacketHeader(routePacket.routeHeader)
                            dispatch(item)
                        }
                    } catch (e: Exception) {
                        sessionSender.errorReply(routePacket.routeHeader, Common.BaseErrorCode.SYSTEM_ERROR_VALUE.toShort())
                        LOG.error(ExceptionUtils.getStackTrace(e),this,e)
                    }
                }else{
                    isUsing.set(false)
                }
            }
        }
    }
    // from backend server
    fun dispatch(packet: RoutePacket) {
        val msgName = packet.msgId
        val isBase = packet.isBase()

        if(isBase){

            when(msgName){
                AuthenticateMsg.getDescriptor().index -> {
                    val authenticateMsg = AuthenticateMsg.parseFrom(packet.data())
                    val apiEndpoint = packet.routeHeader.from
                    authenticate(authenticateMsg.serviceId.toShort(),apiEndpoint, authenticateMsg.accountId)
                    LOG.debug("authenticated - accountId:$accountId ,from:$apiEndpoint, sid:$sid",this)
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
                        ReplyPacket(JoinStageInfoUpdateRes.newBuilder().setStageIdx(stageIndex).build()))

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

    private fun updateStageInfo(playEndpoint: String, stageId: Long): Int {

        var stageIndex:Int? = null

        this.playEndpoints.forEach{action->
            if(action.value.stageId == stageId){
                stageIndex = action.key
            }
        }

        if(stageIndex ==null){
            for(i in 1 until 256){
                if(!this.playEndpoints.containsKey(i)){
                    stageIndex = i
                    break
                }
            }
        }

        this.playEndpoints[stageIndex!!] = TargetAddress(playEndpoint,stageId)

        return stageIndex!!
    }
    private fun clearRoomInfo(stageId: Long){
        var stageIndex:Int? = null
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
