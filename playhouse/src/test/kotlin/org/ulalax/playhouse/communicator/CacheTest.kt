package org.ulalax.playhouse.communicator

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import org.ulalax.playhouse.protocol.Server

class CacheTest : FunSpec() {

    private var port = 6379
    private val redisContainer:GenericContainer<Nothing> =  GenericContainer<Nothing>(DockerImageName.parse("redis:6.2.5"))
            .apply {
                withExposedPorts(port)
                start()
            }

    init {

        test("Test ServerInfo update and get") {
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

        test("test timeOver"){
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
}