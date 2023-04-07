package org.ulalax.playhouse.service.play

import org.ulalax.playhouse.communicator.message.RouteHeader
import org.ulalax.playhouse.communicator.message.RoutePacket
import kotlinx.coroutines.runBlocking
import LOG
import org.ulalax.playhouse.protocol.Server.*
import org.ulalax.playhouse.communicator.*
import org.ulalax.playhouse.communicator.message.ProtoPayload
import org.ulalax.playhouse.protocol.Common.BaseErrorCode
import org.ulalax.playhouse.service.BaseSender
import org.ulalax.playhouse.service.play.base.BaseStage
import org.ulalax.playhouse.service.play.base.BaseActor
import org.ulalax.playhouse.service.TimerCallback
import org.ulalax.playhouse.service.TimerManager
import java.lang.Thread.sleep
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

class PlayService(
    override val serviceId:Short,
    private val publicEndpoint:String,
    private val playOption: PlayOption,
    private val clientCommunicator: ClientCommunicator,
    private val requestCache: RequestCache,
    private val serverInfoCenter: ServerInfoCenter) : Service {

    private var state = AtomicReference(ServerState.DISABLE)
    private val baseUsers:MutableMap<Long, BaseActor> = ConcurrentHashMap()
    private val baseRooms:MutableMap<Long, BaseStage> = ConcurrentHashMap()
    private lateinit var threadForCoroutine:Thread
    private val msgQueue = ConcurrentLinkedQueue<RoutePacket>()
    private val timerManager = TimerManager(this)
    private val baseSender = BaseSender(serviceId, clientCommunicator ,requestCache)

    override fun onStart() {
        state.set(ServerState.RUNNING)

        threadForCoroutine = Thread({ messageLoop() },"play:message-loop")
        threadForCoroutine.start()
        timerManager.start()
    }
    fun removeRoom(stageId:Long){
        this.baseRooms.remove(stageId)
    }
    fun removeUser(accountId:Long){
        this.baseUsers.remove(accountId)
    }

    fun errorReply(routeHeader: RouteHeader, errorCode:Short){
        this.baseSender.errorReply(routeHeader,errorCode)
    }

    private fun messageLoop() = runBlocking {
        while(state.get() != ServerState.DISABLE){
            var routePacket = msgQueue.poll()
            while(routePacket!=null){
                routePacket.use {
                    val msgId = routePacket.msgId()
                    val isBase = routePacket.isBase()
                    val stageId = routePacket.routeHeader.stageId
                    val roomPacket = RoutePacket.moveOf(routePacket)

                    if(isBase){
                        doBaseRoomPacket(msgId, roomPacket, stageId)
                    }else{
                        baseRooms[stageId]?.run { this.send(roomPacket) }
                            ?: LOG.error("stageId:$stageId is not exist, msgName:$msgId",this)
                    }
                }
                routePacket = msgQueue.poll()
            }
            sleep(10)
        }
    }

    private suspend fun doBaseRoomPacket(
        msgId: Int,
        routePacket: RoutePacket,
        stageId: Long,
    ) {
        when (msgId) {
            CreateStageReq.getDescriptor().index -> {
                makeBaseRoom(StageIdMaker.makeId()).send(routePacket)
            }

            CreateJoinStageReq.getDescriptor().index -> {
                baseRooms[stageId]?.send(routePacket)
                    ?: makeBaseRoom(stageId).send(routePacket)
            }

            TimerMsg.getDescriptor().index -> {
                val timerId = routePacket.timerId
                val protoPayload = routePacket.getPayload() as ProtoPayload
                timerProcess(stageId, timerId, protoPayload.proto as TimerMsg, routePacket.timerCallback)
            }

            DestroyStage.getDescriptor().index -> {
                baseRooms.remove(stageId)
            }

            else -> {
                var room = baseRooms[stageId]
                if (room == null) {
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
                        room.send(routePacket)
                    }

                    else -> {
                        LOG.error("$msgId is not base packet",this)
                    }
                }
            }

        }
    }

    private fun timerProcess(stageId: Long, timerId:Long,timerMsg: TimerMsg,timerCallback: TimerCallback) {
        baseRooms[stageId]?.run {
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
        }?: LOG.error("room for timer is not exist:$stageId, ${timerMsg.type}",this)
    }

    override fun onReceive(routePacket: RoutePacket) {
            msgQueue.add(routePacket)
    }

    private fun makeBaseRoom(stageId: Long): BaseStage {
        val baseStage = BaseStage(stageId,this,clientCommunicator,requestCache,serverInfoCenter)
        baseRooms[stageId] = baseStage
        return baseStage
    }

    override fun onStop() {
        state.set(ServerState.DISABLE)

    }

    override fun getWeightPoint(): Int {
        return baseUsers.size
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
        return baseUsers[accountId]
    }

    fun addUser(baseActor: BaseActor) {
        baseUsers[baseActor.actorSender.accountId] = baseActor
    }

    fun findRoom(stageId: Long): BaseStage? {
        return baseRooms[stageId]
    }

    fun endpoint(): String {
        return publicEndpoint
    }

    fun cancelTimer(stageId: Long, timerId: Long) {
        baseRooms[stageId]?.cancelTimer(timerId)
    }

    fun createContentRoom(stageType:String, roomSender: BaseStageSender): Stage<Actor> {
        return  playOption.elementConfigurator.rooms[stageType]!!.invoke(roomSender)
    }


    fun createContentUser(stageType: String, userSender: BaseActorSender): Actor {
        return playOption.elementConfigurator.users[stageType]!!.invoke(userSender)
    }

    fun isValidType(stageType: String): Boolean {
        return playOption.elementConfigurator.rooms.contains(stageType)
    }



}