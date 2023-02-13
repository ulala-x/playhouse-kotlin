package org.ulalax.playhouse.service.play

import org.ulalax.playhouse.communicator.message.RouteHeader
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.protocol.ProtoPayload
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.kotlin.logger
import org.ulalax.playhouse.ErrorCode
import org.ulalax.playhouse.protocol.Server.*
import org.ulalax.playhouse.communicator.*
import org.ulalax.playhouse.service.BaseSenderImpl
import org.ulalax.playhouse.service.RequestCache
import org.ulalax.playhouse.service.play.base.BaseStage
import org.ulalax.playhouse.service.play.base.BaseActor
import org.ulalax.playhouse.service.play.base.TimerCallback
import org.ulalax.playhouse.service.play.base.TimerManager
import java.lang.Thread.sleep
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

class PlayService(val serviceId:String,
                  val publicEndpoint:String,
                  val playOption: PlayOption,
                  val communicateClient: CommunicateClient,
                  val requestCache: RequestCache,
                  val serverInfoCenter: ServerInfoCenter,
                  ) : Service {
    private val log = logger()

    private var state = AtomicReference(ServerState.DISABLE)
    private val baseUsers:MutableMap<Long, BaseActor> = ConcurrentHashMap()
    private val baseRooms:MutableMap<Long, BaseStage> = ConcurrentHashMap()
    private lateinit var threadForCoroutine:Thread
    private val msgQueue = ConcurrentLinkedQueue<RoutePacket>()
    private val timerManager = TimerManager(this)
    private val baseSender = BaseSenderImpl(serviceId, communicateClient ,requestCache)

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

    fun errorReply(routeHeader: RouteHeader, errorCode:Int){
        this.baseSender.errorReply(routeHeader,errorCode)
    }

    private fun messageLoop() = runBlocking {
        while(state.get() != ServerState.DISABLE){
            var routePacket = msgQueue.poll()
            while(routePacket!=null){
                routePacket.use {
                    val msgName = routePacket.msgName()
                    val isBase = routePacket.isBase()
                    val stageId = routePacket.routeHeader.stageId
                    val roomPacket = RoutePacket.moveOf(routePacket)

                    if(isBase){
                        doBaseRoomPacket(msgName, roomPacket, stageId)
                    }else{
                        baseRooms[stageId]?.run { this.send(roomPacket) }
                            ?: log.error("stageId:$stageId is not exist, msgName:$msgName")
                    }
                }
                routePacket = msgQueue.poll()
            }
            sleep(10)
        }
    }

    private suspend fun doBaseRoomPacket(
        msgName: String,
        routePacket: RoutePacket,
        stageId: Long,
    ) {
        when (msgName) {
            CreateStageReq.getDescriptor().name -> {
                makeBaseRoom(StageIdMaker.makeId()).send(routePacket)
            }

            CreateJoinStageReq.getDescriptor().name -> {
                baseRooms[stageId]?.send(routePacket)
                    ?: makeBaseRoom(stageId).send(routePacket)
            }

            TimerMsg.getDescriptor().name -> {
                val timerId = routePacket.timerId
                val protoPayload = routePacket.getPayload() as ProtoPayload
                timerProcess(stageId, timerId, protoPayload.proto() as TimerMsg, routePacket.timerCallback)
            }

            DestroyStage.getDescriptor().name -> {
                baseRooms.remove(stageId)
            }

            else -> {

                var room = baseRooms[stageId]
                if (room == null) {
                    log.error(" room is not exist :$stageId,$msgName ")
                    errorReply(routePacket.routeHeader, ErrorCode.STAGE_IS_NOT_EXIST)
                    return
                }

                when (msgName) {
                    JoinStageReq.getDescriptor().name,
                    StageTimer.getDescriptor().name,
                    DisconnectNoticeMsg.getDescriptor().name,
                    AsyncBlock.getDescriptor().name,
                    -> {
                        room.send(routePacket)
                    }

                    else -> {
                        log.error("$msgName is not base packet")
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
                else -> {log.error("Invalid timer type ${type}")}
            }
        }?:log.error("room for timer is not exist:$stageId, ${timerMsg.type}")
    }

    override fun onReceive(routePacket: RoutePacket) {
            msgQueue.add(routePacket)
    }

    private fun makeBaseRoom(stageId: Long): BaseStage {
        val baseStage = BaseStage(stageId,this,communicateClient,requestCache,serverInfoCenter)
        baseRooms[stageId] = baseStage
        return baseStage
    }

    override fun onStop() {
        state.set(ServerState.DISABLE)

    }

    override fun weightPoint(): Int {
        return baseUsers.size
    }

    override fun serverState(): ServerState {
        return state.get()
    }

    override fun serviceType(): ServiceType {
        return ServiceType.Play
    }

    override fun serviceId(): String {
        return this.serviceId
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

    fun createContentRoom(StageType:String, roomSender: StageSenderImpl): Stage<Actor> {
        return  playOption.elementConfigurator.rooms[StageType]!!.invoke(roomSender)
        //clazz.getDeclaredConstructor().newInstance()
        //return roomOption.elementConfigurator.rooms[StageType]!!.constructors.first{it.parameters.size == 1}.call(roomSender) as Room<User>
    }


    fun createContentUser(StageType: String, userSender: ActorSenderImpl): Actor {
        return playOption.elementConfigurator.users[StageType]!!.invoke(userSender)
        //return roomOption.elementConfigurator.users[StageType]!!.constructors.first{it.parameters.size ==1}.call(userSender)
    }

    fun isValidType(StageType: String): Boolean {
        return playOption.elementConfigurator.rooms.contains(StageType)
    }



}