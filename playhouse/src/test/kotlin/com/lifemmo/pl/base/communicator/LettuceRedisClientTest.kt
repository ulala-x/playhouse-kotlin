package com.lifemmo.pl.base.communicator

import com.lifemmo.pl.base.Plbase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import redis.embedded.RedisServer



class LettuceRedisClientTest {
    private lateinit var redisServer: RedisServer
    private val ip = "localhost"
    private val port = TestHelper.findFreePort()


    @BeforeEach
    fun setUp() {
        redisServer = RedisServer.builder().setting("maxmemory 128M").port(port).build()
        redisServer.start()
    }

    @AfterEach
    fun tearDown() {
        redisServer.stop()
    }

    @Test
    fun updateAndGet() {

        val endpoint1 ="127.0.0.1:8081"
        val endpoint2 ="127.0.0.1:8082"

        val redisClient = LettuceRedisClient(ip,port).apply { this.connect() }

        val update1 = Plbase.ServerInfoMsg.newBuilder().setEndpoint(endpoint1)
            .setServiceType(ServiceType.SESSION.name)
            .setServerState(ServerInfo.ServerState.RUNNING.name).setTimestamp(System.currentTimeMillis())
            .setWeightingPoint(0).build()

        val update2 = Plbase.ServerInfoMsg.newBuilder().setEndpoint(endpoint2)
            .setServiceType(ServiceType.API.name)
            .setServerState(ServerInfo.ServerState.RUNNING.name).setTimestamp(System.currentTimeMillis())
            .setWeightingPoint(0).build()

        redisClient.updateServerInfo(ServerInfo.of(update1))
        redisClient.updateServerInfo(ServerInfo.of(update2))

        val serverList = redisClient.getServerList("")
        assertThat(serverList.size).isEqualTo(2)
        assertThat(serverList[0].state).isEqualTo(ServerInfo.ServerState.RUNNING)
        assertThat(serverList).anyMatch { it.bindEndpoint == endpoint1 }.anyMatch{it.bindEndpoint == endpoint2}
    }

    @Test
    fun timeOver(){
        val timestamp = System.currentTimeMillis()

        val serverInfoMsg = Plbase.ServerInfoMsg.newBuilder().setEndpoint("")
            .setServiceType(ServiceType.SESSION.name)
            .setServerState(ServerInfo.ServerState.RUNNING.name).setTimestamp(timestamp)
            .setWeightingPoint(0).build()

        val serverInfo = ServerInfo.of(serverInfoMsg)

        assertThat(serverInfo.timeOver()).isFalse

        serverInfo.timeStamp = timestamp - 59000

        assertThat(serverInfo.timeOver()).isFalse

        serverInfo.timeStamp = timestamp - 61000

        assertThat(serverInfo.timeOver()).isTrue

    }


}