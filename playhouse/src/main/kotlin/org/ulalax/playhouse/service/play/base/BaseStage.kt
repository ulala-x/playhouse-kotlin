package org.ulalax.playhouse.service.play.base

import org.ulalax.playhouse.communicator.CommunicateClient
import org.ulalax.playhouse.communicator.ServerInfoCenter
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.protocol.Packet
import org.ulalax.playhouse.protocol.ReplyPacket
import org.ulalax.playhouse.service.RequestCache
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.logging.log4j.kotlin.logger
import org.ulalax.playhouse.ErrorCode
import org.ulalax.playhouse.protocol.Server.*
import org.ulalax.playhouse.service.play.*
import org.ulalax.playhouse.service.play.base.command.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class BaseStage(
    stageId:Long,
    private val playService: PlayService,
    communicateClient: CommunicateClient,
    reqCache: RequestCache,
    private val serverInfoCenter: ServerInfoCenter,

    ) {
    private val log = logger()
    private val msgHandler = BaseStageCmdHandler()
    private val msgQueue = ConcurrentLinkedQueue<RoutePacket>()
    private val baseStageCoroutineContext = ThreadPoolController.coroutineContext
    private var isUsing = AtomicBoolean(false)
    val stageSenderImpl = StageSenderImpl(playService.serviceId, stageId,playService,communicateClient ,reqCache)


    lateinit var stage: Stage<Actor>
    var isCreated = false

    init {
        msgHandler.register(CreateStageReq.getDescriptor().name, CreateStageCmd(playService))
        msgHandler.register(JoinStageReq.getDescriptor().name, JoinStageCmd(playService))
        msgHandler.register(CreateJoinStageReq.getDescriptor().name, CreateJoinStageCmd(playService))
        msgHandler.register(StageTimer.getDescriptor().name, StageTimerCmd(playService))
        msgHandler.register(DisconnectNoticeMsg.getDescriptor().name, DisconnectNoticeCmd(playService))
        msgHandler.register(AsyncBlock.getDescriptor().name, AsyncBlockCmd<Any>(playService))

    }

    private suspend fun dispatch(routePacket: RoutePacket) {
        stageSenderImpl.setCurrentPacketHeader(routePacket.routeHeader)
        try{
            if(routePacket.isBase()){
                msgHandler.dispatch(this,routePacket)
            }else{
                val accountId = routePacket.accountId()
                val baseUser = playService.findUser(accountId)
                if(baseUser !=null){
                    stage.onDispatch(baseUser.actor , Packet(routePacket.msgName(),routePacket.movePayload()))
                }
            }
        }catch (e:Exception){
            stageSenderImpl.errorReply(routePacket.routeHeader, ErrorCode.SYSTEM_ERROR)
            log.error(ExceptionUtils.getStackTrace(e))
        }finally {
            stageSenderImpl.clearCurrentPacketHeader()
        }

    }

    suspend fun send(routePacket: RoutePacket) = coroutineScope {
        msgQueue.add(routePacket)
        if(isUsing.compareAndSet(false,true)){
            while(isUsing.get()){
                val item = msgQueue.poll()
                if(item!=null) {
                    launch (baseStageCoroutineContext) {
                        try {
                            item.use {
                                dispatch(item)
                            }
                        } catch (e: Exception) {
                            stageSenderImpl.errorReply(routePacket.routeHeader, ErrorCode.UNCHECKED_CONTENTS_ERROR)
                            log.error(ExceptionUtils.getStackTrace(e))
                        }
                    }
                }else{
                    isUsing.set(false)
                }
            }
        }
    }



    suspend fun create(StageType:String, packet: Packet): ReplyPacket {

        this.stage = playService.createContentRoom(StageType,stageSenderImpl)
        this.stageSenderImpl.StageType = StageType
        val outcome  = this.stage.onCreate(packet)
        this.isCreated = true
        return outcome
    }

    suspend fun join(accountId: Long, sessionEndpoint: String, sid: Int,apiEndpoint:String, packet: Packet): ReplyPacket {

        var baseUser = playService.findUser(accountId)

        if (baseUser == null) {
            val userSender = ActorSenderImpl(accountId, sessionEndpoint, sid,apiEndpoint,this,serverInfoCenter)
            val user = playService.createContentUser(this.stageSenderImpl.StageType,userSender)
            baseUser = BaseActor(user, userSender)
            baseUser.actor.onCreate()
            playService.addUser(baseUser)
        } else {
            baseUser.actorSender.update(sessionEndpoint, sid,apiEndpoint)
        }
        val outcome =  stage.onJoinStage(baseUser.actor , packet)
        if(!outcome.isSuccess()){
            playService.removeUser(accountId)
        }else{
            updateSessionRoomInfo(sessionEndpoint, sid)
        }
        return outcome
    }

    private fun updateSessionRoomInfo(sessionEndpoint: String, sid: Int) {
        val joinStageMsg = JoinStageMsg.newBuilder()
            .setStageId(stageId()).setPlayEndpoint(playService.endpoint()).build()

        this.stageSenderImpl.sendToBaseSession(sessionEndpoint,sid,  Packet(joinStageMsg))
    }

    fun reply(packet: ReplyPacket) {
        this.stageSenderImpl.reply(packet)
    }

    fun leaveStage(accountId: Long, sessionEndpoint: String, sid: Int) {
        playService.removeUser(accountId)
        val request = LeaveStageMsg.newBuilder().build()
        this.stageSenderImpl.sendToBaseSession(sessionEndpoint,sid, Packet(request))
    }

    fun stageId(): Long {
       return this.stageSenderImpl.stageId()
    }

    fun cancelTimer(timerId: Long) {
        this.stageSenderImpl.cancelTimer(timerId)
    }

    fun hasTimer(timerId: Long): Boolean {
        return this.stageSenderImpl.hasTimer(timerId)
    }

    suspend fun onPostCreate() {
        try{
            this.stage.onPostCreate()
        }catch (e:Exception){
            log.error(ExceptionUtils.getStackTrace(e))
        }

    }

    suspend fun onPostJoinRoom(accountId: Long) {
        try{
            var baseUser = playService.findUser(accountId)

            //baseUser?.let {
            if(baseUser!=null){
                this.stage.onPostJoinStage(baseUser.actor)
            }else{
                log.error("user is not exist : $accountId")
            }

            //} ?: log.error("user is not exist : $accountId")

        }catch (e:Exception){
            log.error(ExceptionUtils.getStackTrace(e))
        }
    }

    suspend fun onDisconnect(accountId: Long) {
        var baseUser = playService.findUser(accountId)

        //baseUser?.let {
        if(baseUser!=null){
            this.stage.onDisconnect(baseUser.actor)
        }else{
            log.error("user is not exist : $accountId")
        }
    }


}