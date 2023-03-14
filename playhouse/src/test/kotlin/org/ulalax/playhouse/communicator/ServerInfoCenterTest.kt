package org.ulalax.playhouse.communicator

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ServerInfoCenterTest : FunSpec() {

    init {
        val serverList: MutableList<XServerInfo>
        val curTime = System.currentTimeMillis()
        val serverInfoCenter = XServerInfoCenter()

        serverList = mutableListOf(
                XServerInfo.of("tcp://127.0.0.1:0001", ServiceType.API, "api", ServerState.RUNNING, 1, curTime),
                XServerInfo.of("tcp://127.0.0.1:0002", ServiceType.Play, "play", ServerState.RUNNING, 1, curTime),
                XServerInfo.of("tcp://127.0.0.1:0003", ServiceType.SESSION, "session", ServerState.RUNNING, 1, curTime),

                XServerInfo.of("tcp://127.0.0.1:0011", ServiceType.API, "api", ServerState.RUNNING, 11, curTime),
                XServerInfo.of("tcp://127.0.0.1:0012", ServiceType.Play, "play", ServerState.RUNNING, 11, curTime),
                XServerInfo.of("tcp://127.0.0.1:0013", ServiceType.SESSION, "session", ServerState.RUNNING, 11, curTime),

                XServerInfo.of("tcp://127.0.0.1:0021", ServiceType.API, "api", ServerState.RUNNING, 21, curTime),
                XServerInfo.of("tcp://127.0.0.1:0022", ServiceType.Play, "play", ServerState.RUNNING, 21, curTime),
                XServerInfo.of("tcp://127.0.0.1:0023", ServiceType.SESSION, "session", ServerState.RUNNING, 21, curTime)
        )

        test("remove invalid server info from the list") {
            var updatedList = serverInfoCenter.update(serverList)

            updatedList.size shouldBe serverList.size

            val update = listOf(
                    XServerInfo.of("tcp://127.0.0.1:0001",
                            ServiceType.API,"api",
                            ServerState.DISABLE,11,curTime))

            updatedList = serverInfoCenter.update(update)

            updatedList.size shouldBe 1
        }

        test("remove timed-out server info from the list") {
            serverInfoCenter.update(serverList)

            val update = listOf(
                    XServerInfo.of("tcp://127.0.0.1:0011",
                            ServiceType.API,"api",
                            ServerState.RUNNING,1,curTime-61000))

            val updatedList = serverInfoCenter.update(update)

            updatedList.size shouldBe 1
            updatedList[0].state shouldBe ServerState.DISABLE
        }

        test("return the correct server info when searching for an existing server") {
            serverInfoCenter.update(serverList)

            val findServerEndpoint = "tcp://127.0.0.1:0021"
            val serverInfo = serverInfoCenter.findServer(findServerEndpoint)

            withClue("bindEndpoint should be $findServerEndpoint") {
                serverInfo.bindEndpoint shouldBe findServerEndpoint
            }

            withClue("state should be RUNNING") {
                serverInfo.state shouldBe ServerState.RUNNING
            }

            shouldThrow<CommunicatorException.NotExistServerInfo> {
                serverInfoCenter.findServer("")
            }
        }

        test("return the correct round-robin server info") {
            serverInfoCenter.update(serverList)

            withClue("play service should return servers in order 0012 -> 0022 -> 0002") {
                serverInfoCenter.findRoundRobinServer("play").bindEndpoint shouldBe "tcp://127.0.0.1:0012"
                serverInfoCenter.findRoundRobinServer("play").bindEndpoint shouldBe "tcp://127.0.0.1:0022"
                serverInfoCenter.findRoundRobinServer("play").bindEndpoint shouldBe "tcp://127.0.0.1:0002"
            }

            withClue("session service should return servers in order 0013 -> 0023 -> 0003") {
                serverInfoCenter.findRoundRobinServer("session").bindEndpoint shouldBe "tcp://127.0.0.1:0013"
                serverInfoCenter.findRoundRobinServer("session").bindEndpoint shouldBe "tcp://127.0.0.1:0023"
                serverInfoCenter.findRoundRobinServer("session").bindEndpoint shouldBe "tcp://127.0.0.1:0003"
            }
        }

        test("return the full list of server info") {
            serverInfoCenter.update(serverList)
            serverInfoCenter.getServerList().size shouldBe 9
        }
    }
}