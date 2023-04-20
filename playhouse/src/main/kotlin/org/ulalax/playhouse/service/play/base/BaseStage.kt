package org.ulalax.playhouse.service.play.base

import org.ulalax.playhouse.communicator.ClientCommunicator
import org.ulalax.playhouse.communicator.ServerInfoCenter
import org.ulalax.playhouse.communicator.message.RoutePacket
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.apache.commons.lang3.exception.ExceptionUtils
import LOG
import org.ulalax.playhouse.communicator.RequestCache
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.communicator.message.ReplyPacket
import org.ulalax.playhouse.protocol.Common.*
import org.ulalax.playhouse.protocol.Server.*
import org.ulalax.playhouse.service.play.*
import org.ulalax.playhouse.service.play.base.command.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class BaseStage(
    stageId:Long,
    private val playService: PlayProcessor,
    clientCommunicator: ClientCommunicator,
    reqCache: RequestCache,
    private val serverInfoCenter: ServerInfoCenter,
    val stageSender:XStageSender = XStageSender(playService.serviceId, stageId,playService,clientCommunicator ,reqCache)
    ) {

    private val msgHandler = BaseStageCmdHandler()
    private val msgQueue = ConcurrentLinkedQueue<RoutePacket>()
    private var isUsing = AtomicBoolean(false)

    private lateinit var stage: Stage<Actor>
    var isCreated = false

    init {
        msgHandler.register(CreateStageReq.getDescriptor().index, CreateStageCmd(playService))
        msgHandler.register(JoinStageReq.getDescriptor().index, JoinStageCmd(playService))
        msgHandler.register(CreateJoinStageReq.getDescriptor().index, CreateJoinStageCmd(playService))
        msgHandler.register(StageTimer.getDescriptor().index, StageTimerCmd(playService))
        msgHandler.register(DisconnectNoticeMsg.getDescriptor().index, DisconnectNoticeCmd(playService))
        msgHandler.register(AsyncBlock.getDescriptor().index, AsyncBlockCmd(playService))
    }

    private suspend fun dispatch(routePacket: RoutePacket) {
        stageSender.setCurrentPacketHeader(routePacket.routeHeader)
        try{
            if(routePacket.isBase()){
                msgHandler.dispatch(this,routePacket)
            }else{
                val accountId = routePacket.accountId()
                val baseUser = playService.findUser(accountId)
                if(baseUser !=null){
                    stage.onDispatch(baseUser.actor , Packet(routePacket.msgId(),routePacket.movePayload()))
                }
            }
        }catch (e:Exception){
            stageSender.errorReply(routePacket.routeHeader, BaseErrorCode.SYSTEM_ERROR_VALUE.toShort())
            LOG.error(ExceptionUtils.getStackTrace(e),this,e)
        }finally {
            stageSender.clearCurrentPacketHeader()
        }

    }

    suspend fun send(routePacket: RoutePacket) = coroutineScope {
        msgQueue.add(routePacket)
        if(isUsing.compareAndSet(false,true)){
            while(isUsing.get()){
                val item = msgQueue.poll()
                if(item!=null) {
                    try {
                        item.use {
                            dispatch(item)
                        }
                    } catch (e: Exception) {
                        stageSender.errorReply(routePacket.routeHeader, BaseErrorCode.UNCHECKED_CONTENTS_ERROR_VALUE.toShort())
                        LOG.error(ExceptionUtils.getStackTrace(e),this,e)
                    }
                }else{
                    isUsing.set(false)
                }
            }
        }
    }



    suspend fun create(stageType:String, packet: Packet): ReplyPacket {

        this.stage = playService.createContentRoom(stageType,stageSender)
        this.stageSender.stageType = stageType
        val outcome  = this.stage.onCreate(packet)
        this.isCreated = true
        return outcome
    }

    suspend fun join(accountId: Long, sessionEndpoint: String, sid: Int,apiEndpoint:String, packet: Packet): Pair<ReplyPacket,Int> {

        var baseUser = playService.findUser(accountId)

        if (baseUser == null) {
            val userSender = XActorSender(accountId, sessionEndpoint, sid,apiEndpoint,this,serverInfoCenter)
            val user = playService.createContentUser(this.stageSender.stageType,userSender)
            baseUser = BaseActor(user, userSender)
            baseUser.actor.onCreate()
            playService.addUser(baseUser)
        } else {
            baseUser.actorSender.update(sessionEndpoint, sid,apiEndpoint)
        }
        val outcome =  stage.onJoinStage(baseUser.actor , packet)
        var stageIndex = 0
        if(!outcome.isSuccess()){
            playService.removeUser(accountId)
        }else{
            stageIndex= updateSessionRoomInfo(sessionEndpoint, sid)
        }
        return Pair(outcome,stageIndex)
    }

    private suspend fun updateSessionRoomInfo(sessionEndpoint: String, sid: Int): Int{
        val joinStageInfoUpdateReq = JoinStageInfoUpdateReq.newBuilder()
            .setStageId(stageId()).setPlayEndpoint(playService.endpoint()).build()

        val res = this.stageSender.requestToBaseSession(sessionEndpoint,sid,  Packet(joinStageInfoUpdateReq))
        val result = JoinStageInfoUpdateRes.parseFrom(res.data())
        return result.stageIdx

    }

    fun reply(packet: ReplyPacket) {
        this.stageSender.reply(packet)
    }

    fun leaveStage(accountId: Long, sessionEndpoint: String, sid: Int) {
        playService.removeUser(accountId)
        val request = LeaveStageMsg.newBuilder().build()
        this.stageSender.sendToBaseSession(sessionEndpoint,sid, Packet(request))
    }

    fun stageId(): Long {
       return this.stageSender.stageId()
    }

    fun cancelTimer(timerId: Long) {
        this.stageSender.cancelTimer(timerId)
    }

    fun hasTimer(timerId: Long): Boolean {
        return this.stageSender.hasTimer(timerId)
    }

    suspend fun onPostCreate() {
        try{
            this.stage.onPostCreate()
        }catch (e:Exception){
            LOG.error(ExceptionUtils.getStackTrace(e),this,e)
        }

    }

    suspend fun onPostJoinRoom(accountId: Long) {
        try{
            var baseUser = playService.findUser(accountId)

            if(baseUser!=null){
                this.stage.onPostJoinStage(baseUser.actor)
            }else{
                LOG.error("user is not exist : $accountId",this)
            }

        }catch (e:Exception){
            LOG.error(ExceptionUtils.getStackTrace(e),this,e)
        }
    }

    suspend fun onDisconnect(accountId: Long) {
        var baseUser = playService.findUser(accountId)

        //baseUser?.let {
        if(baseUser!=null){
            this.stage.onDisconnect(baseUser.actor)
        }else{
            LOG.error("user is not exist : $accountId",this)
        }
    }
}
