package org.ulalax.playhouse.service.play

import com.google.protobuf.ByteString
import org.ulalax.playhouse.communicator.ServerInfoCenter
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.protocol.Packet
import org.ulalax.playhouse.service.RequestCache
import org.ulalax.playhouse.service.SpyCommunicateClient
import org.ulalax.playhouse.service.play.base.TimerCallback
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.kotlin.logger
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.ulalax.playhouse.ErrorCode
import org.ulalax.playhouse.protocol.Server.*
import java.time.Duration

class StageServiceTest {

    val bindEndpoint = "tcp://127.0.0.1:8777"
    val resultList = mutableListOf<RoutePacket>()
    val StageType = "dungeon"
    lateinit var playService: PlayService
    val teststageId = 10000L
    private val log = logger()

    @BeforeEach
    fun setUp() {
        ThreadPoolController.coroutineContext

        val communicateClient = SpyCommunicateClient(resultList)
        val reqCache = RequestCache(5)

        val playOption = PlayOption().apply {
            this.elementConfigurator.register(StageType,
                { stageSender -> StageStub(stageSender) },
                { actorSender -> ActorStub(actorSender) })
        }
//        val roomOption = RoomOption().apply {
//            this.elementConfigurator.register(StageType,RoomStub::class,UserStub::class)
//        }

        val serverInfoCenter: ServerInfoCenter = mock()

        playService = PlayService("play", bindEndpoint, playOption, communicateClient, reqCache,serverInfoCenter)
        playService.onStart()
    }
    @AfterEach
    fun tearDown() {
        resultList.clear()
    }

    private fun createRoomPacket(StageType: String): RoutePacket {
        val packet = Packet(
            CreateStageReq.newBuilder()
                .setStageType(StageType)
                .setPayloadName("contentCreateRoom")
                .setPayload(ByteString.EMPTY).build()
        )
        return RoutePacket.stageOf(0, 0, packet, isBase = true, isBackend = true)
            .apply { setMsgSeq(1) }
    }

    private fun joinRoomPacket(stageId:Long,accountId:Long): RoutePacket {
        val req = JoinStageReq.newBuilder()
            .setSessionEndpoint("tcp://127.0.0.1:5555")
            .setSid(1)
            .setPayloadName("contentJoinRoom")
            .setPayload(ByteString.EMPTY).build()

        val packet = Packet(req)
        return RoutePacket.stageOf(stageId,accountId,packet,isBase = true,isBackend = true)
            .apply { setMsgSeq(2) }
    }

    private fun createJoinRoomPacket(StageType: String,stageId:Long,accountId:Long): RoutePacket {
        val req = CreateJoinStageReq.newBuilder()
            .setStageType(StageType)
            .setSessionEndpoint("tcp://127.0.0.1:5555")
            .setSid(1)
            .setCreatePayloadName("contentCreateRoom")
            .setCreatePayload(ByteString.EMPTY)
            .setJoinPayloadName("contentJoinRoom")
            .setJoinPayload(ByteString.EMPTY)
            .build()

        val packet = Packet(req)
        return RoutePacket.stageOf(stageId,accountId,packet,isBase = true,isBackend = true)
            .apply { setMsgSeq(3) }
    }

    @Test
    fun create_room_with_success(): Long {
        var routePacket = createRoomPacket(StageType)

        playService.onReceive(routePacket)
        Thread.sleep(100)
        assertThat(resultList.size).isEqualTo(1)
        val createStageRes = CreateStageRes.parseFrom(resultList[0].buffer())
        assertThat(createStageRes.payloadName).isEqualTo("contentCreateRoom")
        return createStageRes.stageId

    }

    @Test
    fun create_room_with_invalid_type(){
        val routePacket = createRoomPacket("invalid type")
        playService.onReceive(routePacket)
        Thread.sleep(100)
        assertThat(resultList.size).isEqualTo(1)
        assertThat(resultList[0].routeHeader.header.baseErrorCode).isEqualTo(ErrorCode.STAGE_TYPE_IS_INVALID)
    }

    @Test
    fun join_room_test_with_success(){
        val stageId = create_room_with_success()
        val joinRoom  = joinRoomPacket(stageId,100)
        playService.onReceive(joinRoom)
        Thread.sleep(100)
        assertThat(resultList.size).isEqualTo(3)
        assertThat(JoinStageRes.parseFrom(resultList[2].buffer()).payloadName).isEqualTo("contentJoinRoom")
    }

    @Test
    fun join_room_test_with_invalid_stageId(){

        val joinRoom  = joinRoomPacket(99,100)
        playService.onReceive(joinRoom)
        Thread.sleep(100)
        assertThat(resultList.size).isEqualTo(1)
        assertThat(resultList[0].routeHeader.header.baseErrorCode).isEqualTo(ErrorCode.STAGE_IS_NOT_EXIST)
    }

    @Test
    fun create_join_room_with_create_room(){
        val createJoinRoom = createJoinRoomPacket(StageType,teststageId,1000)
        playService.onReceive(createJoinRoom)
        Thread.sleep(100)
        assertThat(resultList.size).isEqualTo(2)
        val createJoinStageRes = CreateJoinStageRes.parseFrom(resultList[1].buffer())
        assertThat(resultList[1].msgName()).isEqualTo(CreateJoinStageRes.getDescriptor().name)
        assertThat(createJoinStageRes.isCreated).isTrue
        assertThat(createJoinStageRes.createPayloadName).isEqualTo("contentCreateRoom")
        assertThat(createJoinStageRes.joinPayloadName).isEqualTo("contentJoinRoom")

    }

    @Test
    fun create_join_room_with_join_room() {
        val stageId = create_room_with_success()

        val createJoinRoom = createJoinRoomPacket(StageType, stageId, 1000)
        playService.onReceive(createJoinRoom)
        Thread.sleep(100)

        assertThat(resultList.size).isEqualTo(3)
        val createJoinStageRes = CreateJoinStageRes.parseFrom(resultList[2].buffer())

        assertThat(resultList[2].msgName()).isEqualTo(CreateJoinStageRes.getDescriptor().name)
        assertThat(createJoinStageRes.isCreated).isFalse
        assertThat(createJoinStageRes.createPayloadName).isEqualTo("")
        assertThat(createJoinStageRes.joinPayloadName).isEqualTo("contentJoinRoom")
    }

    @Test
    fun add_repeat_timer(){
        create_join_room_with_create_room()
        Thread.sleep(100)
        val room = playService.findRoom(teststageId)!!

        var resultCount = 0
        room.stageSenderImpl.addRepeatTimer(Duration.ZERO, Duration.ofMillis(100),object: TimerCallback {
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
        val room = playService.findRoom(teststageId)!!

        var resultCount = 0
        val timerId =
            room.stageSenderImpl.addCountTimer(Duration.ZERO, 3, Duration.ofMillis(10), object : TimerCallback {
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
        val room = playService.findRoom(teststageId)!!

        var resultCount = 0
        val timerId = room.stageSenderImpl.addRepeatTimer(Duration.ZERO, Duration.ofMillis(100),object: TimerCallback {
            override suspend fun invoke() {
                resultCount++
            }
        })

        Thread.sleep(50)
        room.stageSenderImpl.cancelTimer(timerId)
        Thread.sleep(200)
        assertThat(resultCount).isEqualTo(1)
    }

    @Test
    fun asyncBlock():Unit = runBlocking{
        create_join_room_with_create_room()
        Thread.sleep(100)
        val room = playService.findRoom(teststageId)!!
        var result = ""

        room.stageSenderImpl.asyncBlock({"test async block"},
            {pass -> result = pass}
        )

        Thread.sleep(50)
        assertThat(result).isEqualTo("test async block")

    }

    @Test
    fun onStart() {

//        val roomCommonOption = CommonOption().apply {
//            this.port = 7777
//            this.serviceId = "play"
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