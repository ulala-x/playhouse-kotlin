package org.ulalax.playhouse.service.play

import com.google.protobuf.ByteString
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import org.ulalax.playhouse.communicator.ServerInfoCenter
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.service.SpyClientCommunicator
import org.ulalax.playhouse.service.TimerCallback
import org.mockito.kotlin.mock
import org.ulalax.playhouse.communicator.RequestCache
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.protocol.Common.BaseErrorCode
import org.ulalax.playhouse.protocol.Server.*
import java.time.Duration

class StageServiceTest : FunSpec() {

    private val bindEndpoint = "tcp://127.0.0.1:8777"
    private val resultList = mutableListOf<RoutePacket>()
    private val StageType = "dungeon"
    private lateinit var playService: PlayService
    private val teststageId = 10000L

    init {

        beforeTest {
            ThreadPoolController.coroutineContext

            val communicateClient = SpyClientCommunicator(resultList)
            val reqCache = RequestCache(5)

            val playOption = PlayOption().apply {
                this.elementConfigurator.register(StageType,
                        { stageSender -> StageStub(stageSender) },
                        { actorSender -> ActorStub(actorSender) })
            }

            val serverInfoCenter: ServerInfoCenter = mock()

            playService = PlayService(2, bindEndpoint, playOption, communicateClient, reqCache,serverInfoCenter)
            playService.onStart()
        }

        afterTest{
            resultList.clear()
        }

        test("create room should be success") {
            // given
            val routePacket = createRoomPacket(StageType)

            // when
            playService.onReceive(routePacket)
            Thread.sleep(200)

            // then
            val response = resultList[0]
            response.routeHeader.header.errorCode.shouldBe(BaseErrorCode.SUCCESS.number)

            val createStageRes = CreateStageRes.parseFrom(response.data())
            createStageRes.payloadId shouldBe 0
        }



        test("create room with Invalid Type should be get invalid error") {
            val routePacket = createRoomPacket("invalid type")
            playService.onReceive(routePacket)
            Thread.sleep(200)

            resultList[0].routeHeader.header.errorCode.shouldBe(BaseErrorCode.STAGE_TYPE_IS_INVALID.number)
        }

        test(" join room should be success"){
            //given
            val stageId = create_room_with_success()
            val joinRoom  = joinRoomPacket(stageId,100)

            //when
            playService.onReceive(joinRoom)
            Thread.sleep(200)

            //then
            resultList.shouldHaveSize(3)
            JoinStageRes.parseFrom(resultList[2].data()).payloadId.shouldBe(2)
        }

        //    @Test
        test("join room with invalid id ,should get stage is not exist error"){

            val joinRoom  = joinRoomPacket(99,100)
            playService.onReceive(joinRoom)
            Thread.sleep(200)

            resultList.shouldHaveSize(1)
            resultList[0].routeHeader.header.errorCode.shouldBe(BaseErrorCode.STAGE_IS_NOT_EXIST.number)

        }


        test("create join room in create state should be success"){
            val createJoinRoom = createJoinRoomPacket(StageType,teststageId,1000)
            playService.onReceive(createJoinRoom)

            Thread.sleep(200)

            resultList.shouldHaveSize(2)

            val createJoinStageRes = CreateJoinStageRes.parseFrom(resultList[1].data())
            resultList[1].msgId().shouldBe(CreateJoinStageRes.getDescriptor().index)

            createJoinStageRes.isCreated.shouldBeTrue()
            createJoinStageRes.createPayloadId.shouldBe(1)
            createJoinStageRes.joinPayloadId.shouldBe(2)

        }


        test("create join room in join state should be success") {
            val stageId = create_room_with_success()

            val createJoinRoom = createJoinRoomPacket(StageType, stageId, 1000)
            playService.onReceive(createJoinRoom)
            Thread.sleep(100)

            resultList.shouldHaveSize(3)
            val createJoinStageRes = CreateJoinStageRes.parseFrom(resultList[2].data())

            resultList[2].msgId().shouldBe(CreateJoinStageRes.getDescriptor().index)
            createJoinStageRes.isCreated.shouldBeFalse()
            createJoinStageRes.createPayloadId.shouldBe(0)
            createJoinStageRes.joinPayloadId.shouldBe(2)
        }


        test("add_repeat_timer"){
            create_join_room_with_create_room()
            Thread.sleep(100)
            val room = playService.findRoom(teststageId)!!

            var resultCount = 0
            room.stageSenderImpl.addRepeatTimer(Duration.ZERO, Duration.ofMillis(100),object: TimerCallback {
                override suspend fun invoke() {
                    resultCount++
                }
            })

            Thread.sleep(450)
            resultCount.shouldBe(5)
        }

        test("add_count_timer"){
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

            Thread.sleep(100)
            resultCount.shouldBe(3)
        }


        test("cancel_timer"){
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
            resultCount.shouldBe(1)
        }


        test("fun asyncBlock():Unit = runBlocking"){
            create_join_room_with_create_room()
            Thread.sleep(100)
            val room = playService.findRoom(teststageId)!!
            var result = ""

            room.stageSenderImpl.asyncBlock({"test async block"},
                {pass -> result = pass}
            )

            Thread.sleep(50)
            result.shouldBe("test async block")

        }

    }

    private fun createRoomPacket(StageType: String): RoutePacket {
        val packet = Packet(
            CreateStageReq.newBuilder()
                .setStageType(StageType).build()
        )
        return RoutePacket.stageOf(0, 0, packet, isBase = true, isBackend = true)
            .apply { setMsgSeq(1) }
    }

    private fun joinRoomPacket(stageId:Long,accountId:Long): RoutePacket {
        val req = JoinStageReq.newBuilder()
            .setSessionEndpoint("tcp://127.0.0.1:5555")
            .setSid(1)
            .setPayloadId(2)
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
            .setCreatePayloadId(1)
            .setCreatePayload(ByteString.EMPTY)
            .setJoinPayloadId(2)
            .setJoinPayload(ByteString.EMPTY)
            .build()

        val packet = Packet(req)
        return RoutePacket.stageOf(stageId,accountId,packet,isBase = true,isBackend = true)
            .apply { setMsgSeq(3) }
    }

    private fun create_room_with_success(): Long {
        var routePacket = createRoomPacket(StageType)

        playService.onReceive(routePacket)
        Thread.sleep(200)

        val createStageRes = CreateStageRes.parseFrom(resultList[0].data())
        return createStageRes.stageId
    }

    private fun create_join_room_with_create_room(){
        val createJoinRoom = createJoinRoomPacket(StageType,teststageId,1000)
        playService.onReceive(createJoinRoom)

        Thread.sleep(200)
    }
}