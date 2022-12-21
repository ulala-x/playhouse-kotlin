package org.ulalax.playhouse.base.service.room

import com.google.protobuf.ByteString
import org.ulalax.playhouse.base.communicator.ServerInfoCenter
import org.ulalax.playhouse.base.communicator.message.RoutePacket
import org.ulalax.playhouse.base.protocol.Packet
import org.ulalax.playhouse.base.service.RequestCache
import org.ulalax.playhouse.base.service.SpyCommunicateClient
import org.ulalax.playhouse.base.service.room.base.TimerCallback
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.kotlin.logger
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.ulalax.playhouse.base.ErrorCode
import org.ulalax.playhouse.base.Server
import org.ulalax.playhouse.base.Server.*
import java.time.Duration

class RoomServiceTest {

    val bindEndpoint = "tcp://127.0.0.1:8777"
    val resultList = mutableListOf<RoutePacket>()
    val roomType = "dungeon"
    lateinit var roomService:RoomService
    val testRoomId = 10000L
    private val log = logger()

    @BeforeEach
    fun setUp() {
        ThreadPoolController.coroutineContext

        val communicateClient = SpyCommunicateClient(resultList)
        val reqCache = RequestCache(5)

        val roomOption = RoomOption().apply {
            this.elementConfigurator.register(roomType,
                { roomSender -> RoomStub(roomSender) },
                { userSender -> UserStub(userSender) })
        }
//        val roomOption = RoomOption().apply {
//            this.elementConfigurator.register(roomType,RoomStub::class,UserStub::class)
//        }

        val serverInfoCenter:ServerInfoCenter = mock()

        roomService = RoomService("room", bindEndpoint, roomOption, communicateClient, reqCache,serverInfoCenter)
        roomService.onStart()
    }
    @AfterEach
    fun tearDown() {
        resultList.clear()
    }

    private fun createRoomPacket(roomType: String): RoutePacket {
        val packet = Packet(
            CreateRoomReq.newBuilder()
                .setRoomType(roomType)
                .setPayloadName("contentCreateRoom")
                .setPayload(ByteString.EMPTY).build()
        )
        return RoutePacket.roomOf(0, 0, packet, isBase = true, isBackend = true)
            .apply { setMsgSeq(1) }
    }

    private fun joinRoomPacket(roomId:Long,accountId:Long): RoutePacket {
        val req = JoinRoomReq.newBuilder()
            .setSessionEndpoint("tcp://127.0.0.1:5555")
            .setSid(1)
            .setPayloadName("contentJoinRoom")
            .setPayload(ByteString.EMPTY).build()

        val packet = Packet(req)
        return RoutePacket.roomOf(roomId,accountId,packet,isBase = true,isBackend = true)
            .apply { setMsgSeq(2) }
    }

    private fun createJoinRoomPacket(roomType: String,roomId:Long,accountId:Long): RoutePacket {
        val req = CreateJoinRoomReq.newBuilder()
            .setRoomType(roomType)
            .setSessionEndpoint("tcp://127.0.0.1:5555")
            .setSid(1)
            .setCreatePayloadName("contentCreateRoom")
            .setCreatePayload(ByteString.EMPTY)
            .setJoinPayloadName("contentJoinRoom")
            .setJoinPayload(ByteString.EMPTY)
            .build()

        val packet = Packet(req)
        return RoutePacket.roomOf(roomId,accountId,packet,isBase = true,isBackend = true)
            .apply { setMsgSeq(3) }
    }

    @Test
    fun create_room_with_success(): Long {
        var routePacket = createRoomPacket(roomType)

        roomService.onReceive(routePacket)
        Thread.sleep(100)
        assertThat(resultList.size).isEqualTo(1)
        val createRoomRes = CreateRoomRes.parseFrom(resultList[0].buffer())
        assertThat(createRoomRes.payloadName).isEqualTo("contentCreateRoom")
        return createRoomRes.roomId

    }

    @Test
    fun create_room_with_invalid_type(){
        val routePacket = createRoomPacket("invalid type")
        roomService.onReceive(routePacket)
        Thread.sleep(100)
        assertThat(resultList.size).isEqualTo(1)
        assertThat(resultList[0].routeHeader.header.baseErrorCode).isEqualTo(ErrorCode.ROOM_TYPE_IS_INVALID)
    }

    @Test
    fun join_room_test_with_success(){
        val roomId = create_room_with_success()
        val joinRoom  = joinRoomPacket(roomId,100)
        roomService.onReceive(joinRoom)
        Thread.sleep(100)
        assertThat(resultList.size).isEqualTo(3)
        assertThat(JoinRoomRes.parseFrom(resultList[2].buffer()).payloadName).isEqualTo("contentJoinRoom")
    }

    @Test
    fun join_room_test_with_invalid_roomId(){

        val joinRoom  = joinRoomPacket(99,100)
        roomService.onReceive(joinRoom)
        Thread.sleep(100)
        assertThat(resultList.size).isEqualTo(1)
        assertThat(resultList[0].routeHeader.header.baseErrorCode).isEqualTo(ErrorCode.ROOM_IS_NOT_EXIST)
    }

    @Test
    fun create_join_room_with_create_room(){
        val createJoinRoom = createJoinRoomPacket(roomType,testRoomId,1000)
        roomService.onReceive(createJoinRoom)
        Thread.sleep(100)
        assertThat(resultList.size).isEqualTo(2)
        val createJoinRoomRes = CreateJoinRoomRes.parseFrom(resultList[1].buffer())
        assertThat(resultList[1].msgName()).isEqualTo(CreateJoinRoomRes.getDescriptor().name)
        assertThat(createJoinRoomRes.isCreated).isTrue
        assertThat(createJoinRoomRes.createPayloadName).isEqualTo("contentCreateRoom")
        assertThat(createJoinRoomRes.joinPayloadName).isEqualTo("contentJoinRoom")

    }

    @Test
    fun create_join_room_with_join_room() {
        val roomId = create_room_with_success()

        val createJoinRoom = createJoinRoomPacket(roomType, roomId, 1000)
        roomService.onReceive(createJoinRoom)
        Thread.sleep(100)

        assertThat(resultList.size).isEqualTo(3)
        val createJoinRoomRes = CreateJoinRoomRes.parseFrom(resultList[2].buffer())

        assertThat(resultList[2].msgName()).isEqualTo(CreateJoinRoomRes.getDescriptor().name)
        assertThat(createJoinRoomRes.isCreated).isFalse
        assertThat(createJoinRoomRes.createPayloadName).isEqualTo("")
        assertThat(createJoinRoomRes.joinPayloadName).isEqualTo("contentJoinRoom")
    }

    @Test
    fun add_repeat_timer(){
        create_join_room_with_create_room()
        Thread.sleep(100)
        val room = roomService.findRoom(testRoomId)!!

        var resultCount = 0
        room.roomSender.addRepeatTimer(Duration.ZERO, Duration.ofMillis(100),object:TimerCallback{
            override suspend fun invoke() {
                resultCount++
            }
        })

        Thread.sleep(510)
        assertThat(resultCount).isEqualTo(5)
    }

    @Test
    fun add_count_timer(){
        create_join_room_with_create_room()
        Thread.sleep(100)
        val room = roomService.findRoom(testRoomId)!!

        var resultCount = 0
        val timerId =
            room.roomSender.addCountTimer(Duration.ZERO, 3, Duration.ofMillis(10), object : TimerCallback {
                override suspend fun invoke() {
                    resultCount++
                }
            })

        //Thread.sleep(50)
        //room.roomSender.cancelTimer(timerId)

        Thread.sleep(100)
        assertThat(resultCount).isEqualTo(3)
    }

    @Test
    fun cancel_timer(){
        create_join_room_with_create_room()
        Thread.sleep(100)
        val room = roomService.findRoom(testRoomId)!!

        var resultCount = 0
        val timerId = room.roomSender.addRepeatTimer(Duration.ZERO, Duration.ofMillis(100),object:TimerCallback{
            override suspend fun invoke() {
                resultCount++
            }
        })

        Thread.sleep(50)
        room.roomSender.cancelTimer(timerId)
        Thread.sleep(200)
        assertThat(resultCount).isEqualTo(1)
    }

    @Test
    fun asyncBlock():Unit = runBlocking{
        create_join_room_with_create_room()
        Thread.sleep(100)
        val room = roomService.findRoom(testRoomId)!!
        var result = ""

        room.roomSender.asyncBlock({"test async block"},
            {pass -> result = pass}
        )

        Thread.sleep(50)
        assertThat(result).isEqualTo("test async block")

    }

    @Test
    fun onStart() {

//        val roomCommonOption = CommonOption().apply {
//            this.port = 7777
//            this.serviceId = "room"
//            this.redisPort = redisPort
//            this.serverSystem = object : ServerSystem {
//                override fun onDispatch(packet: Packet) {
//                    TODO("Not yet implemented")
//                }
//            }
//            this.requestTimeoutSec = 5
//        }
//
//        val roomOption = RoomOption().apply {
//            this.elementConfigurator.register("dungeon",
//                { roomSender -> RoomStub(roomSender) },
//                { userSender -> UserStub(userSender) })
//        }
//
//        val roomSever = RoomServer(roomCommonOption, roomOption)
//
//        val roomThead = Thread{
//            roomSever.start()
//            roomSever.awaitTermination()
//        }
//
//        roomThead.start()

//        val apiCommonOption = CommonOption().apply {
//            this.serviceId = "api"
//            this.port = 8888
//            this.serverSystem = object:ServerSystem{
//                override fun onDispatch(packet: Packet) {
//                    TODO("Not yet implemented")
//                }
//            }
//            this.redisPort = redisPort
//            this.requestTimeoutSec = 5
//        }
//
//        val apiOption = ApiOption().apply {
//            this.apiPath = RoomServiceTest::class.java.packageName
//            this.apiCallBackHandler = object :ApiCallBackHandler{
//                override fun onDisconnect(accountId: Long, sessionInfo: String) {
//                    TODO("Not yet implemented")
//                }
//            }
//            this.executorService = Executors.newFixedThreadPool(1)
//        }
//
//        val apiServer = ApiServer(apiCommonOption,apiOption)
//        val apiThread = Thread{
//            apiServer.start()
//            apiServer.awaitTermination()
//        }
//        apiThread.start()

    }

}