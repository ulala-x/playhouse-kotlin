package org.ulalax.playhouse.service.play

import org.ulalax.playhouse.communicator.message.RouteHeader
import org.ulalax.playhouse.communicator.message.RoutePacket
import LOG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ulalax.playhouse.protocol.Server.*
import org.ulalax.playhouse.communicator.*
import org.ulalax.playhouse.communicator.message.ProtoPayload
import org.ulalax.playhouse.protocol.Common.BaseErrorCode
import org.ulalax.playhouse.service.XSender
import org.ulalax.playhouse.service.play.base.BaseStage
import org.ulalax.playhouse.service.play.base.BaseActor
import org.ulalax.playhouse.service.TimerCallback
import org.ulalax.playhouse.service.TimerManager
import java.lang.Thread.sleep
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

class PlayProcessor(
    override val serviceId:Short,
    private val publicEndpoint:String,
    private val playOption: PlayOption,
    private val clientCommunicator: ClientCommunicator,
    private val requestCache: RequestCache,
    private val serverInfoCenter: ServerInfoCenter,

) : Processor {
    private var state = AtomicReference(ServerState.DISABLE)
    private val baseActors:MutableMap<Long, BaseActor> = ConcurrentHashMap()
    private val baseStages:MutableMap<Long, BaseStage> = ConcurrentHashMap()
    private lateinit var threadForCoroutine:Thread
    private val msgQueue = ConcurrentLinkedQueue<RoutePacket>()
    private val timerManager = TimerManager(this)
    private val sender :XSender = XSender(serviceId, clientCommunicator ,requestCache)

    override fun onStart() {
        state.set(ServerState.RUNNING)

        threadForCoroutine = Thread({ messageLoop() },"play:message-loop")
        threadForCoroutine.start()
    }
    fun removeRoom(stageId:Long){
        this.baseStages.remove(stageId)
    }
    fun removeUser(accountId:Long){
        this.baseActors.remove(accountId)
    }

    fun errorReply(routeHeader: RouteHeader, errorCode:Short){
        this.sender.errorReply(routeHeader,errorCode)
    }

    private fun messageLoop() {
        val coroutineDispatcher = Dispatchers.IO
        val scope = CoroutineScope(coroutineDispatcher)

        while(state.get() != ServerState.DISABLE){
            var routePacket = msgQueue.poll()
            while(routePacket!=null){
                routePacket.use {
                    val msgId = routePacket.msgId
                    val isBase = routePacket.isBase()
                    val stageId = routePacket.routeHeader.stageId
                    val accountId = routePacket.accountId
                    val roomPacket = RoutePacket.moveOf(routePacket)

                    if(isBase){
                        scope.launch{
                            doBaseStagePacket(msgId, roomPacket, stageId)
                        }
                    }else{
                        scope.launch{
                            baseStages[stageId]?.run { this.send(roomPacket) }
                                ?: LOG.error("stageId:$stageId is not exist,accountId:${accountId}, msgId:$msgId",this)
                        }
                    }
                }
                routePacket = msgQueue.poll()
            }
            sleep(10)
        }
    }

    private suspend fun doBaseStagePacket(
        msgId: Int,
        routePacket: RoutePacket,
        stageId: Long,
    ) {
        when (msgId) {
            CreateStageReq.getDescriptor().index -> {
                if(baseStages.contains(stageId)){
                    errorReply(routePacket.routeHeader,BaseErrorCode.ALREADY_EXIST_STAGE_VALUE.toShort())
                }else{
                    makeBaseStage(stageId).send(routePacket)
                }
            }

            CreateJoinStageReq.getDescriptor().index -> {
                baseStages[stageId]?.send(routePacket)
                    ?: makeBaseStage(stageId).send(routePacket)
            }

            TimerMsg.getDescriptor().index -> {
                val timerId = routePacket.timerId
                val protoPayload = routePacket.getPayload() as ProtoPayload
                timerProcess(stageId, timerId, protoPayload.proto as TimerMsg, routePacket.timerCallback)
            }

            DestroyStage.getDescriptor().index -> {
                baseStages.remove(stageId)
            }

            else -> {
                val baseStage = baseStages[stageId]
                if (baseStage == null) {
                    LOG.error(" room is not exist :$stageId,$msgId",this)
                    errorReply(routePacket.routeHeader, BaseErrorCode.STAGE_IS_NOT_EXIST_VALUE.toShort())
                    return
                }

                when (msgId) {
                    JoinStageReq.getDescriptor().index,
                    StageTimer.getDescriptor().index,
                    DisconnectNoticeMsg.getDescriptor().index,
                    AsyncBlock.getDescriptor().index,
                    -> {
                        baseStage.send(routePacket)
                    }

                    else -> {
                        LOG.error("$msgId is not base packet",this)
                    }
                }
            }

        }
    }

    private fun timerProcess(stageId: Long, timerId:Long,timerMsg: TimerMsg,timerCallback: TimerCallback) {
        baseStages[stageId]?.run {
            when(val type = timerMsg.type){
                TimerMsg.Type.REPEAT ->{
                    timerManager.registerRepeatTimer(
                        stageId,
                        timerId,
                        timerMsg.initialDelay,
                        timerMsg.period,
                        timerCallback)
                }
                TimerMsg.Type.COUNT ->{
                    timerManager.registerCountTimer(
                        stageId,
                        timerId,
                        timerMsg.initialDelay,
                        timerMsg.count,
                        timerMsg.period,
                        timerCallback,
                    )

                }
                TimerMsg.Type.CANCEL ->{
                    timerManager.cancelTimer(timerId)

                }
                else -> {
                    LOG.error("Invalid timer type $type",this)}
            }
        }?: LOG.error("stage for timer is not exist - stageId:$stageId, timerType:${timerMsg.type}",this)
    }

    override fun onReceive(routePacket: RoutePacket) {
            msgQueue.add(routePacket)
    }

    private fun makeBaseStage(stageId: Long): BaseStage {
        val baseStage = BaseStage(stageId,this,clientCommunicator,requestCache,serverInfoCenter)
        baseStages[stageId] = baseStage
        return baseStage
    }

    override fun onStop() {
        state.set(ServerState.DISABLE)

    }

    override fun getWeightPoint(): Int {
        return baseActors.size
    }

    override fun getServerState(): ServerState {
        return state.get()
    }

    override fun getServiceType(): ServiceType {
        return ServiceType.Play
    }

    override fun pause() {
        this.state.set(ServerState.PAUSE)
    }

    override fun resume() {
        this.state.set(ServerState.RUNNING)
    }

    fun findUser(accountId: Long): BaseActor? {
        return baseActors[accountId]
    }

    fun addUser(baseActor: BaseActor) {
        baseActors[baseActor.actorSender.accountId] = baseActor
    }

    fun findRoom(stageId: Long): BaseStage? {
        return baseStages[stageId]
    }

    fun endpoint(): String {
        return publicEndpoint
    }

    fun cancelTimer(stageId: Long, timerId: Long) {
        baseStages[stageId]?.cancelTimer(timerId)
    }

    fun createContentStage(stageType:String, stageSender: XStageSender): Stage<Actor> {
        return  playOption.elementConfigurator.stages[stageType]!!.invoke(stageSender)
    }

    fun createContentActor(stageType: String, actorSender: XActorSender): Actor {
        return playOption.elementConfigurator.actors[stageType]!!.invoke(actorSender)
    }

    fun isValidType(stageType: String): Boolean {
        return playOption.elementConfigurator.stages.contains(stageType)
    }
}