package org.ulalax.playhouse.service.session

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.mockk.mockk
import io.mockk.verify
import org.ulalax.playhouse.communicator.message.RoutePacket
import io.netty.channel.Channel
import org.ulalax.playhouse.communicator.*
import org.ulalax.playhouse.communicator.message.ClientPacket
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.protocol.Server.AuthenticateMsg


internal class SessionClientTest : FunSpec() {

    private lateinit var serviceCenter: ServerInfoCenter ;
    private lateinit var channel:Channel
    private lateinit var reqCache: RequestCache
    private lateinit var clientCommunicator: ClientCommunicator
    private val serviceId:Short = 1
    private val api:Short = 2
    private val urls = arrayListOf<String>()
    private val sid = 1
    private val testMsgId = 1


    init {
        beforeTest {
            serviceCenter = XServerInfoCenter()

            serviceCenter.update(
                listOf(XServerInfo.of("tcp://127.0.0.1:0021",
                    ServiceType.API,api,
                    ServerState.RUNNING,
                    21,
                    System.currentTimeMillis())
                )
            )

            channel = mockk(relaxed = true)
            reqCache = RequestCache(0)
            clientCommunicator = mockk(relaxed = true)
        }

        afterTest {
        }


        test("without authenticate send packet ,socket should be disconnected" ){
            val sessionClient = SessionClient(serviceId,sid,channel,serviceCenter,clientCommunicator,urls,reqCache)
            val clientPacket = ClientPacket.toServerOf(api, Packet(testMsgId))
            sessionClient.onReceive(clientPacket)
            verify(exactly = 1){channel.disconnect()}
        }


        test("the packet on the auth list should be delivered") {

            urls.add("$api:$testMsgId")

            val sessionClient = SessionClient(serviceId,sid,channel,serviceCenter,clientCommunicator,urls,reqCache)
            sessionClient.onReceive(ClientPacket.toServerOf(api, Packet(testMsgId)))

            verify (exactly = 1) {clientCommunicator.send(any(),any())}
        }


        test("receive authenticate packet ,session client should be authenticated"){
            //api 서버로부터 authenticate 패킷을 받을 경우 인증 확인 및 session info 정보 확인

            //given
            val accountId = 1000L
            val message = AuthenticateMsg.newBuilder()
                    .setServiceId(api.toInt())
                    .setAccountId(accountId)
                    .build()

            val sessionClient = SessionClient(serviceId,sid,channel,serviceCenter,clientCommunicator,urls,reqCache)

            //when
            sessionClient.onReceive(RoutePacket.sessionOf(sid, Packet(message), isBase = true, isBackend = true))

            //then
            sessionClient.isAuthenticated.shouldBeTrue()
        }

    }
}