package com.lifemmo.pl.base.service.room

import com.lifemmo.pl.base.BaseErrorCode
import com.lifemmo.pl.base.Plbase.*
import com.lifemmo.pl.base.communicator.*
import com.lifemmo.pl.base.communicator.message.RouteHeader
import com.lifemmo.pl.base.communicator.message.RoutePacket
import com.lifemmo.pl.base.protocol.ProtoPayload
import com.lifemmo.pl.base.service.*
import com.lifemmo.pl.base.service.room.base.*
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.kotlin.logger
import java.lang.Thread.sleep
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

class RoomService(val serviceId:String,
                  val publicEndpoint:String,
                  val roomOption: RoomOption,
                  val communicateClient: CommunicateClient,
                  val requestCache: RequestCache,
                  val serverInfoCenter: ServerInfoCenter,
                  ) : Service {
    private val log = logger()

    private var state = AtomicReference(ServerInfo.ServerState.DISABLE)
    private val baseUsers:MutableMap<Long, BaseUser> = ConcurrentHashMap()
    private val baseRooms:MutableMap<Long, BaseRoom> = ConcurrentHashMap()
    private lateinit var threadForCoroutine:Thread
    private val msgQueue = ConcurrentLinkedQueue<RoutePacket>()
    private val timerManager = TimerManager(this)
    private val baseSender = BaseSenderImpl(serviceId, communicateClient ,requestCache)

    override fun onStart() {
        state.set(ServerInfo.ServerState.RUNNING)

        threadForCoroutine = Thread{ messageLoop() }
        threadForCoroutine.start()
        timerManager.start()
    }
    fun removeRoom(roomId:Long){
        this.baseRooms.remove(roomId)
    }
    fun removeUser(accountId:Long){
        this.baseUsers.remove(accountId)
    }

    fun errorReply(routeHeader: RouteHeader, errorCode:Int){
        this.baseSender.errorReply(routeHeader,errorCode)
    }

    private fun messageLoop() = runBlocking {
        while(state.get() != ServerInfo.ServerState.DISABLE){
            var routePacket = msgQueue.poll()
            while(routePacket!=null){
                routePacket.use {
                    val msgName = routePacket.msgName()
                    val isBase = routePacket.isBase()
                    val roomId = routePacket.routeHeader.roomId
                    val roomPacket = RoutePacket.moveOf(routePacket)

                    if(isBase){
                        doBaseRoomPacket(msgName, roomPacket, roomId)
                    }else{
                        baseRooms[roomId]?.run { this.send(roomPacket) }
                            ?: log.error("roomId:$roomId is not exist, msgName:$msgName")
                    }

                    routePacket = msgQueue.poll()
                }
            }
            sleep(10)
        }
    }

    private suspend fun doBaseRoomPacket(
        msgName: String,
        routePacket: RoutePacket,
        roomId: Long,
    ) {
        when (msgName) {
            CreateRoomReq.getDescriptor().name -> {
                makeBaseRoom(RoomIdMaker.makeId()).send(routePacket)
            }

            CreateJoinRoomReq.getDescriptor().name -> {
                baseRooms[roomId]?.send(routePacket)
                    ?: makeBaseRoom(roomId).send(routePacket)
            }

            TimerMsg.getDescriptor().name -> {
                val timerId = routePacket.timerId
                val protoPayload = routePacket.getPayload() as ProtoPayload
                timerProcess(roomId, timerId, protoPayload.proto() as TimerMsg, routePacket.timerCallback)
            }

            DestroyRoom.getDescriptor().name -> {
                baseRooms.remove(roomId)
            }

            else -> {

                var room = baseRooms[roomId]
                if (room == null) {
                    log.error(" room is not exist :$roomId,$msgName ")
                    errorReply(routePacket.routeHeader, BaseErrorCode.ROOM_IS_NOT_EXIST)
                    return
                }

                when (msgName) {
                    JoinRoomReq.getDescriptor().name,
                    RoomTimer.getDescriptor().name,
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

    private fun timerProcess(roomId: Long, timerId:Long,timerMsg: TimerMsg,timerCallback: TimerCallback) {
        baseRooms[roomId]?.run {
            when(val type = timerMsg.type){
                TimerMsg.Type.REPEAT ->{
                    timerManager.registerRepeatTimer(
                        roomId,
                        timerId,
                        timerMsg.initialDelay,
                        timerMsg.period,
                        timerCallback)
                }
                TimerMsg.Type.COUNT ->{
                    timerManager.registerCountTimer(
                        roomId,
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
        }?:log.error("room for timer is not exist:$roomId, ${timerMsg.type}")
    }

    override fun onReceive(routePacket: RoutePacket) {
            msgQueue.add(routePacket)
    }

    private fun makeBaseRoom(roomId: Long): BaseRoom {
        val baseRoom = BaseRoom(roomId,this,communicateClient,requestCache,serverInfoCenter)
        baseRooms[roomId] = baseRoom
        return baseRoom
    }

    override fun onStop() {
        state.set(ServerInfo.ServerState.DISABLE)

    }

    override fun weightPoint(): Int {
        return baseUsers.size
    }

    override fun serverState(): ServerInfo.ServerState {
        return state.get()
    }

    override fun serviceType(): ServiceType {
        return ServiceType.ROOM
    }

    override fun serviceId(): String {
        return this.serviceId
    }

    override fun pause() {
        this.state.set(ServerInfo.ServerState.PAUSE)
    }

    override fun resume() {
        this.state.set(ServerInfo.ServerState.RUNNING)
    }

    fun findUser(accountId: Long): BaseUser? {
        return baseUsers[accountId]
    }

    fun addUser(baseUser: BaseUser) {
        baseUsers[baseUser.userSender.accountId] = baseUser
    }

    fun findRoom(roomId: Long): BaseRoom? {
        return baseRooms[roomId]
    }

    fun endpoint(): String {
        return publicEndpoint
    }

    fun cancelTimer(roomId: Long, timerId: Long) {
        baseRooms[roomId]?.cancelTimer(timerId)
    }

    fun createContentRoom(roomType:String, roomSender: RoomSenderImpl): Room<User> {
        return  roomOption.elementConfigurator.rooms[roomType]!!.invoke(roomSender)
        //clazz.getDeclaredConstructor().newInstance()
        //return roomOption.elementConfigurator.rooms[roomType]!!.constructors.first{it.parameters.size == 1}.call(roomSender) as Room<User>
    }


    fun createContentUser(roomType: String, userSender: UserSenderImpl): User {
        return roomOption.elementConfigurator.users[roomType]!!.invoke(userSender)
        //return roomOption.elementConfigurator.users[roomType]!!.constructors.first{it.parameters.size ==1}.call(userSender)
    }

    fun isValidType(roomType: String): Boolean {
        return roomOption.elementConfigurator.rooms.contains(roomType)
    }



}