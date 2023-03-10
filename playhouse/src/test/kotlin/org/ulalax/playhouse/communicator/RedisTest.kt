package org.ulalax.playhouse.communicator

import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.TestFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import org.ulalax.playhouse.protocol.Server

@Testcontainers
class SingleRedisTest : AnnotationSpec() {

    private var port = 6379

    @Container
    val redisContainer:GenericContainer<Nothing> =  GenericContainer<Nothing>(DockerImageName.parse("redis:6.2.5"))
            .apply {
                withExposedPorts(port)
            }

    @BeforeAll
    fun setup() {
        redisContainer.start()
    }

    @AfterAll
    fun cleanup() {
        redisContainer.stop()
    }

    @TestFactory
    fun `Test ServerInfo update and get`() {
        val redisCacheClient = RedisCacheClient(redisContainer.host,redisContainer.getMappedPort(port))
        redisCacheClient.connect()

        val endpoint1 ="127.0.0.1:8081"
        val endpoint2 ="127.0.0.1:8082"

        val update1 = Server.ServerInfoMsg.newBuilder().setEndpoint(endpoint1)
                .setServiceType(ServiceType.SESSION.name)
                .setServerState(ServerState.RUNNING.name).setTimestamp(System.currentTimeMillis())
                .setWeightingPoint(0).build()

        val update2 = Server.ServerInfoMsg.newBuilder().setEndpoint(endpoint2)
                .setServiceType(ServiceType.API.name)
                .setServerState(ServerState.RUNNING.name).setTimestamp(System.currentTimeMillis())
                .setWeightingPoint(0).build()

        redisCacheClient.updateServerInfo(XServerInfo.of(update1))
        redisCacheClient.updateServerInfo(XServerInfo.of(update2))

        val serverList = redisCacheClient.getServerList("")


        serverList shouldHaveSize 2
        serverList[0].state shouldBe ServerState.RUNNING
        serverList.should {
            it.any { baseServerInfo -> baseServerInfo.bindEndpoint == endpoint1 }
            it.any { baseServerInfo -> baseServerInfo.bindEndpoint == endpoint2 }
        }

        redisCacheClient.close()
    }
    @TestFactory
    fun timeOver(){
        val timestamp = System.currentTimeMillis()

        val serverInfoMsg = Server.ServerInfoMsg.newBuilder().setEndpoint("")
                .setServiceType(ServiceType.SESSION.name)
                .setServerState(ServerState.RUNNING.name).setTimestamp(timestamp)
                .setWeightingPoint(0).build()

        val serverInfo = XServerInfo.of(serverInfoMsg)

        serverInfo.timeOver() shouldBe false
        serverInfo.lastUpdate = timestamp - 59000
        serverInfo.timeOver() shouldBe false
        serverInfo.lastUpdate = timestamp - 61000
        serverInfo.timeOver() shouldBe true
    }
}