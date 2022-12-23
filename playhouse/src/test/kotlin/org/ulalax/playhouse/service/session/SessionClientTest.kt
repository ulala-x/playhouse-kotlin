package org.ulalax.playhouse.service.session

import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.protocol.ClientPacket
import org.ulalax.playhouse.protocol.Packet
import org.ulalax.playhouse.service.RequestCache
import org.ulalax.playhouse.service.SpyCommunicateClient
import io.netty.channel.Channel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.ulalax.playhouse.communicator.*
import org.ulalax.playhouse.protocol.Server.AuthenticateMsg


internal class SessionClientTest {

    lateinit var serviceCenter: ServerInfoCenter
    lateinit var channel:Channel
    lateinit var reqCache: RequestCache
    lateinit var communicateClient: CommunicateClient
    val serviceId = "session"
    val resultList = mutableListOf<RoutePacket>()
    val urls = arrayListOf<String>()
    val sid = 1


    @BeforeEach
    fun setUp() {
        serviceCenter = mock()
        channel = mock()
        reqCache = RequestCache(0)
        communicateClient = SpyCommunicateClient(resultList)


    }

    @AfterEach
    fun tearDown() {
        resultList.clear()
    }

    @Test
    fun relay_packet_with_non_register_packet_without_authenticate() {
        val sessionClient = SessionClient(serviceId,sid,channel,serviceCenter,communicateClient,urls,reqCache)
        val clientPacket = ClientPacket.toServerOf("api", Packet("test"))
        sessionClient.onReceive(clientPacket)
        verify(channel){channel.disconnect()}
    }

    @Test
    fun relay_packet_with_register_packet_without_authenticate() {

        urls.add("api:test")
        whenever(serviceCenter.findRoundRobinServer("api")).thenReturn(
            ServerInfoImpl.of("tcp://127.0.0.1:0021", ServiceType.API,"api", ServerState.RUNNING,21,System.currentTimeMillis())
        )

        val sessionClient = SessionClient(serviceId,sid,channel,serviceCenter,communicateClient,urls,reqCache)
        val clientPacket = ClientPacket.toServerOf("api", Packet("test"))
        sessionClient.onReceive(clientPacket)
        assertThat(resultList.size).isEqualTo(1)
    }

    @Test
    fun authenticate_from_api_server(){
        //api 서버로부터 authenticate 패킷을 받을 경우 인증 확인 및 session info 정보 확인
        val accountId = 1000L
        val sessionInfo = "session infos"

        val message = AuthenticateMsg.newBuilder()
            .setServiceId("api")
            .setAccountId(accountId)
            .setSessionInfo(sessionInfo).build()
        val routePacket = RoutePacket.sessionOf(sid, Packet(message), isBase = true, isBackend = true)

        val sessionClient = SessionClient(serviceId,sid,channel,serviceCenter,communicateClient,urls,reqCache)
        sessionClient.onReceive(routePacket)

        assertThat(sessionClient.isAuthenticated).isTrue
        assertThat(sessionClient.getSessionInfo("api")).isEqualTo(sessionInfo)
    }




}