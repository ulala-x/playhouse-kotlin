package org.ulalax.playhouse.service.play

import com.google.protobuf.ByteString
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.ulalax.playhouse.communicator.ClientCommunicator
import org.ulalax.playhouse.communicator.RequestCache
import org.ulalax.playhouse.communicator.ServerInfoCenter
import org.ulalax.playhouse.communicator.message.AsyncBlockPacket
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.communicator.message.ReplyPacket
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.protocol.Common
import org.ulalax.playhouse.protocol.Server
import org.ulalax.playhouse.protocol.Test.TestMsg
import org.ulalax.playhouse.service.play.base.BaseStage

class StageTest : FunSpec() {

    //private val resultList = mutableListOf<RoutePacket>()
    private val stageType = "dungeon"
    private lateinit var playProcessor:PlayProcessor
    private val testStageId = 10000L
    private val sessionEndpoint = "tcp://127.0.0.1:5555"
    private val bindEndpoint = "tcp://127.0.0.1:8777"
    private lateinit var stage:BaseStage
    private lateinit var xStageSender:XStageSender
    private lateinit var clientCommunicator: ClientCommunicator
    private var contentStage:Stage<Actor> = mockk(relaxed = true)
    private var stageId:Long = 1

    init {

        beforeTest {
            clientCommunicator = mockk()
            val reqCache = RequestCache(0)
            val playOption = PlayOption().apply {
                this.elementConfigurator.register(stageType,
                    { stageSender -> contentStage },
                    { actorSender -> mockk(relaxed = true) })
            }
            val serverInfoCenter: ServerInfoCenter = mockk()

            playProcessor = PlayProcessor(2, bindEndpoint, playOption, clientCommunicator, reqCache,mockk())
            xStageSender = spyk(XStageSender(2,stageId,playProcessor,clientCommunicator,reqCache))
            stage = spyk(BaseStage(stageId,playProcessor,clientCommunicator,reqCache,serverInfoCenter,xStageSender),recordPrivateCalls = true)

            coEvery { stage["updateSessionRoomInfo"](any<String>(),any<Int>()) } returns 1
            coEvery {contentStage.onCreate(any())} returns ReplyPacket(0,TestMsg.newBuilder().setTestMsg("onCreate").build())
            coEvery {contentStage.onJoinStage(any(),any())} returns ReplyPacket(0,TestMsg.newBuilder().setTestMsg("onJoinStage").build())

        }

        afterTest{
            //resultList.clear()
        }

        test("create room should be success") {
            // given
            val slotPacket = slot<RoutePacket>()
            every { clientCommunicator.send(any(),capture(slotPacket)) } just Runs

            // when
            stage.send(createRoomPacket(stageType))

            //then
            val result = slotPacket.captured

            result.routeHeader.header.errorCode.shouldBe(Common.BaseErrorCode.SUCCESS.number)

            result.msgId() shouldBe Server.CreateStageRes.getDescriptor().index
            val createStageRes = Server.CreateStageRes.parseFrom(result.data())

            createStageRes.payloadId shouldBe TestMsg.getDescriptor().index

            TestMsg.parseFrom(createStageRes.payload).testMsg shouldBe "onCreate"
        }

        test("create room with Invalid Type should be get invalid error") {

            //given
            val slotPacket = slot<RoutePacket>()
            every { clientCommunicator.send(any(),capture(slotPacket)) } just Runs

            //when
            stage.send(createRoomPacket("invalid type"))

            //then
            val result = slotPacket.captured

            result.routeHeader.header.errorCode.shouldBe(Common.BaseErrorCode.STAGE_TYPE_IS_INVALID.number)
        }

        test(" join room should be success"){

            //given
            create_room_with_success()

            val slotPacket = slot<RoutePacket>()
            every { clientCommunicator.send(any(),capture(slotPacket)) } just Runs

            //when
            stage.send(joinRoomPacket(stageId,100))
            val result = slotPacket.captured

            //then
            result.msgId() shouldBe Server.JoinStageRes.getDescriptor().index
            val joinStageRes = Server.JoinStageRes.parseFrom(result.data())

            joinStageRes.payloadId shouldBe TestMsg.getDescriptor().index
            TestMsg.parseFrom(joinStageRes.payload).testMsg shouldBe "onJoinStage"
        }

        test("create join room in create state should be success"){

            val createJoinRoom = createJoinRoomPacket(stageType,testStageId,1000)
            val slotPacket = slot<RoutePacket>()
            every { clientCommunicator.send(any(),capture(slotPacket)) } just Runs

            stage.send(createJoinRoom)

            val result = slotPacket.captured

            result.msgId().shouldBe(Server.CreateJoinStageRes.getDescriptor().index)
            val createJoinStageRes = Server.CreateJoinStageRes.parseFrom(result.data())

            createJoinStageRes.isCreated.shouldBeTrue()
            createJoinStageRes.createPayloadId shouldBe TestMsg.getDescriptor().index
            createJoinStageRes.joinPayloadId shouldBe TestMsg.getDescriptor().index

            TestMsg.parseFrom(createJoinStageRes.createPayload).testMsg shouldBe "onCreate"
            TestMsg.parseFrom(createJoinStageRes.joinPayload).testMsg shouldBe "onJoinStage"
        }


        test("create join room in join state should be success") {
            create_room_with_success()

            val slotPacket = slot<RoutePacket>()
            every { clientCommunicator.send(any(),capture(slotPacket)) } just Runs

            stage.send(createJoinRoomPacket(stageType, stageId, 1000))

            val result = slotPacket.captured

            result.msgId().shouldBe(Server.CreateJoinStageRes.getDescriptor().index)

            val createJoinStageRes = Server.CreateJoinStageRes.parseFrom(result.data())
            createJoinStageRes.isCreated shouldBe false
            createJoinStageRes.createPayloadId shouldBe 0
            createJoinStageRes.joinPayloadId shouldBe TestMsg.getDescriptor().index
        }

        test("fun asyncBlock():Unit = runBlocking"){
            var result = ""
            stage.send(AsyncBlockPacket.of(stageId, { pass->result = pass },"test async block"))
            result.shouldBe("test async block")
        }

    }

    private fun createRoomPacket(StageType: String): RoutePacket {
        val packet = Packet(
            Server.CreateStageReq.newBuilder()
                .setStageType(StageType).build()
        )
        return RoutePacket.stageOf(0, 0, packet, isBase = true, isBackend = true)
            .apply { setMsgSeq(1) }
    }

    private fun joinRoomPacket(stageId:Long,accountId:Long): RoutePacket {
        val req = Server.JoinStageReq.newBuilder()
            .setSessionEndpoint(sessionEndpoint)
            .setSid(1)
            .setPayloadId(2)
            .setPayload(ByteString.EMPTY).build()

        val packet = Packet(req)
        return RoutePacket.stageOf(stageId,accountId,packet,isBase = true,isBackend = true)
            .apply { setMsgSeq(2) }
    }

    private fun createJoinRoomPacket(StageType: String,stageId:Long,accountId:Long): RoutePacket {
        val req = Server.CreateJoinStageReq.newBuilder()
            .setStageType(StageType)
            .setSessionEndpoint(sessionEndpoint)
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

    private suspend fun create_room_with_success() {

        val slotPacket = slot<RoutePacket>()
        every { clientCommunicator.send(any(),capture(slotPacket)) } just Runs

        stage.send(createRoomPacket(stageType))

        val result = slotPacket.captured

        result.routeHeader.header.errorCode.shouldBe(Common.BaseErrorCode.SUCCESS.number)

        val createStageRes = Server.CreateStageRes.parseFrom(result.data())

    }


}