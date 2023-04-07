package org.ulalax.playhouse.service.session

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.service.SpyClientCommunicator
import io.netty.channel.Channel
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.ulalax.playhouse.communicator.*
import org.ulalax.playhouse.communicator.message.ClientPacket
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.protocol.Server.AuthenticateMsg


internal class SessionClientTest : FunSpec() {

    private lateinit var serviceCenter: ServerInfoCenter
    private lateinit var channel:Channel
    private lateinit var reqCache: RequestCache
    private lateinit var clientCommunicator: ClientCommunicator
    private val serviceId:Short = 1
    private val api:Short = 2
    private val resultList = mutableListOf<RoutePacket>()
    private val urls = arrayListOf<String>()
    private val sid = 1
    private val testMsgId = 1


    init {
        beforeTest {
            serviceCenter = mock()
            channel = mock()
            reqCache = RequestCache(0)
            clientCommunicator = SpyClientCommunicator(resultList)
        }

        afterTest {
            resultList.clear()
        }


        test("without authenticate send packet ,socket should be disconnected" ){
            val sessionClient = SessionClient(serviceId,sid,channel,serviceCenter,clientCommunicator,urls,reqCache)
            val clientPacket = ClientPacket.toServerOf(api, Packet(testMsgId))
            sessionClient.onReceive(clientPacket)
            verify(channel){channel.disconnect()}
        }


        test("the packet on the auth list should be delivered") {

            urls.add("$api:$testMsgId")
            whenever(serviceCenter.findRoundRobinServer(api)).thenReturn(
                    XServerInfo.of("tcp://127.0.0.1:0021", ServiceType.API,api, ServerState.RUNNING,21,System.currentTimeMillis())
            )

            val sessionClient = SessionClient(serviceId,sid,channel,serviceCenter,clientCommunicator,urls,reqCache)
            val clientPacket = ClientPacket.toServerOf(api, Packet(testMsgId))
            sessionClient.onReceive(clientPacket)
            resultList.shouldHaveSize(1)
        }


        test("receive authenticate packet ,session client should be authenticated"){
            //api 서버로부터 authenticate 패킷을 받을 경우 인증 확인 및 session info 정보 확인
            val accountId = 1000L
            val sessionInfo = "session infos"

            val message = AuthenticateMsg.newBuilder()
                    .setServiceId(api.toInt())
                    .setAccountId(accountId)
                    .setSessionInfo(sessionInfo).build()
            val routePacket = RoutePacket.sessionOf(sid, Packet(message), isBase = true, isBackend = true)

            val sessionClient = SessionClient(serviceId,sid,channel,serviceCenter,clientCommunicator,urls,reqCache)
            sessionClient.onReceive(routePacket)

            sessionClient.isAuthenticated.shouldBeTrue()
            sessionClient.getSessionInfo(api).shouldBe(sessionInfo)
        }

    }
}