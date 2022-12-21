package org.ulalax.playhouse.base.service.room.base

import org.ulalax.playhouse.base.communicator.CommunicateClient
import org.ulalax.playhouse.base.communicator.ServerInfoCenter
import org.ulalax.playhouse.base.communicator.message.RoutePacket
import org.ulalax.playhouse.base.protocol.Packet
import org.ulalax.playhouse.base.protocol.ReplyPacket
import org.ulalax.playhouse.base.service.RequestCache
import org.ulalax.playhouse.base.service.room.*
import org.ulalax.playhouse.base.service.room.base.command.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.logging.log4j.kotlin.logger
import org.ulalax.playhouse.base.ErrorCode
import org.ulalax.playhouse.base.Server.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class BaseRoom(
    roomId:Long,
    private val roomService: RoomService,
    communicateClient: CommunicateClient,
    reqCache:RequestCache,
    private val serverInfoCenter: ServerInfoCenter,

    ) {
    private val log = logger()
    private val msgHandler = BaseRoomCmdHandler()
    private val msgQueue = ConcurrentLinkedQueue<RoutePacket>()
    private val baseRoomCoroutineContext = ThreadPoolController.coroutineContext
    private var isUsing = AtomicBoolean(false)
    val roomSender = RoomSenderImpl(roomService.serviceId, roomId,roomService,communicateClient ,reqCache)


    lateinit var room:Room<User>
    var isCreated = false

    init {
        msgHandler.register(CreateRoomReq.getDescriptor().name,CreateRoomCmd(roomService))
        msgHandler.register(JoinRoomReq.getDescriptor().name,JoinRoomCmd(roomService))
        msgHandler.register(CreateJoinRoomReq.getDescriptor().name, CreateJoinRoomCmd(roomService))
        msgHandler.register(RoomTimer.getDescriptor().name, RoomTimerCmd(roomService))
        msgHandler.register(DisconnectNoticeMsg.getDescriptor().name, DisconnectNoticeCmd(roomService))
        msgHandler.register(AsyncBlock.getDescriptor().name,AsyncBlockCmd<Any>(roomService))

    }

    private suspend fun dispatch(routePacket: RoutePacket) {
        roomSender.setCurrentPacketHeader(routePacket.routeHeader)
        try{
            if(routePacket.isBase()){
                msgHandler.dispatch(this,routePacket)
            }else{
                val accountId = routePacket.accountId()
                val baseUser = roomService.findUser(accountId)
                if(baseUser !=null){
                    room.onDispatch(baseUser.user ,Packet(routePacket.msgName(),routePacket.movePayload()))
                }
            }
        }catch (e:Exception){
            roomSender.errorReply(routePacket.routeHeader, ErrorCode.SYSTEM_ERROR)
            log.error(ExceptionUtils.getStackTrace(e))
        }finally {
            roomSender.clearCurrentPacketHeader()
        }

    }

    suspend fun send(routePacket: RoutePacket) = coroutineScope {
        msgQueue.add(routePacket)
        if(isUsing.compareAndSet(false,true)){
            while(isUsing.get()){
                val item = msgQueue.poll()
                if(item!=null) {
                    launch (baseRoomCoroutineContext) {
                        try {
                            item.use {
                                dispatch(item)
                            }
                        } catch (e: Exception) {
                            roomSender.errorReply(routePacket.routeHeader, ErrorCode.UNCHECKED_CONTENTS_ERROR)
                            log.error(ExceptionUtils.getStackTrace(e))
                        }
                    }
                }else{
                    isUsing.set(false)
                }
            }
        }
    }



    suspend fun create(roomType:String, packet: Packet):ReplyPacket{

        this.room = roomService.createContentRoom(roomType,roomSender)
        this.roomSender.roomType = roomType
        val outcome  = this.room.onCreate(packet)
        this.isCreated = true
        return outcome
    }

    suspend fun join(accountId: Long, sessionEndpoint: String, sid: Int,apiEndpoint:String, packet: Packet): ReplyPacket {

        var baseUser = roomService.findUser(accountId)

        if (baseUser == null) {
            val userSender = UserSenderImpl(accountId, sessionEndpoint, sid,apiEndpoint,this,serverInfoCenter)
            val user = roomService.createContentUser(this.roomSender.roomType,userSender)
            baseUser = BaseUser(user, userSender)
            baseUser.user.onCreate()
            roomService.addUser(baseUser)
        } else {
            baseUser.userSender.update(sessionEndpoint, sid,apiEndpoint)
        }
        val outcome =  room.onJoinRoom(baseUser.user , packet)
        if(!outcome.isSuccess()){
            roomService.removeUser(accountId)
        }else{
            updateSessionRoomInfo(sessionEndpoint, sid)
        }
        return outcome
    }

    private fun updateSessionRoomInfo(sessionEndpoint: String, sid: Int) {
        val joinRoomMsg = JoinRoomMsg.newBuilder()
            .setRoomId(roomId()).setRoomEndpoint(roomService.endpoint()).build()

        this.roomSender.sendToBaseSession(sessionEndpoint,sid,  Packet(joinRoomMsg))
    }

    fun reply(packet: ReplyPacket) {
        this.roomSender.reply(packet)
    }

    fun leaveRoom(accountId: Long, sessionEndpoint: String, sid: Int) {
        roomService.removeUser(accountId)
        val request = LeaveRoomMsg.newBuilder().build()
        this.roomSender.sendToBaseSession(sessionEndpoint,sid,Packet(request))
    }

    fun roomId(): Long {
       return this.roomSender.roomId()
    }

    fun cancelTimer(timerId: Long) {
        this.roomSender.cancelTimer(timerId)
    }

    fun hasTimer(timerId: Long): Boolean {
        return this.roomSender.hasTimer(timerId)
    }

    suspend fun onPostCreate() {
        try{
            this.room.onPostCreate()
        }catch (e:Exception){
            log.error(ExceptionUtils.getStackTrace(e))
        }

    }

    suspend fun onPostJoinRoom(accountId: Long) {
        try{
            var baseUser = roomService.findUser(accountId)

            //baseUser?.let {
            if(baseUser!=null){
                this.room.onPostJoinRoom(baseUser.user)
            }else{
                log.error("user is not exist : $accountId")
            }

            //} ?: log.error("user is not exist : $accountId")

        }catch (e:Exception){
            log.error(ExceptionUtils.getStackTrace(e))
        }
    }

    suspend fun onDisconnect(accountId: Long) {
        var baseUser = roomService.findUser(accountId)

        //baseUser?.let {
        if(baseUser!=null){
            this.room.onDisconnect(baseUser.user)
        }else{
            log.error("user is not exist : $accountId")
        }
    }


}
