package org.ulalax.playhouse.communicator

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ServerInfoCenterTest {

    private lateinit var serverInfoCenter: ServerInfoCenterImpl
    private lateinit var serverList:MutableList<ServerInfoImpl>
    private val curTime = System.currentTimeMillis()
    private val endpoint = "tcp://127.0.0.1:7777"
    @BeforeEach
    fun setUp() {

        serverList = mutableListOf(
            ServerInfoImpl.of("tcp://127.0.0.1:0001", ServiceType.API,"api", ServerState.RUNNING,1,curTime),
            ServerInfoImpl.of("tcp://127.0.0.1:0002", ServiceType.Play,"play", ServerState.RUNNING,1,curTime),
            ServerInfoImpl.of("tcp://127.0.0.1:0003",
                ServiceType.SESSION,"session",
                ServerState.RUNNING,1,curTime),

            ServerInfoImpl.of("tcp://127.0.0.1:0011", ServiceType.API,"api", ServerState.RUNNING,11,curTime),
            ServerInfoImpl.of("tcp://127.0.0.1:0012", ServiceType.Play,"play", ServerState.RUNNING,11,curTime),
            ServerInfoImpl.of("tcp://127.0.0.1:0013",
                ServiceType.SESSION,"session",
                ServerState.RUNNING,11,curTime),

            ServerInfoImpl.of("tcp://127.0.0.1:0021", ServiceType.API,"api", ServerState.RUNNING,21,curTime),
            ServerInfoImpl.of("tcp://127.0.0.1:0022", ServiceType.Play,"play", ServerState.RUNNING,21,curTime),
            ServerInfoImpl.of("tcp://127.0.0.1:0023",
                ServiceType.SESSION,"session",
                ServerState.RUNNING,21,curTime),
        )

        serverInfoCenter = ServerInfoCenterImpl()

    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    fun updateWithDisable() {
        var updatedList = serverInfoCenter.update(serverList)

        assertThat(updatedList.size).isEqualTo(serverList.size)

        val update = listOf(
            ServerInfoImpl.of("tcp://127.0.0.1:0001",
                ServiceType.API,"api",
                ServerState.DISABLE,11,curTime))

        updatedList = serverInfoCenter.update(update)

        assertThat(updatedList.size).isEqualTo(1)
    }

    @Test
    fun updateWithTimeout() {

        serverInfoCenter.update(serverList)

        val update = listOf(
            ServerInfoImpl.of("tcp://127.0.0.1:0011",
                ServiceType.API,"api",
                ServerState.RUNNING,1,curTime-61000))

        val updatedList =  serverInfoCenter.update(update)

        assertThat(updatedList.size).isEqualTo(1)
        assertThat(updatedList[0].state).isEqualTo(ServerState.DISABLE)
    }


    @Test
    fun findServer() {
        serverInfoCenter.update(serverList)

        val findServerEndpoint = "tcp://127.0.0.1:0021"
        val serverInfo = serverInfoCenter.findServer(findServerEndpoint)
        assertThat(serverInfo.bindEndpoint).isEqualTo(findServerEndpoint)
        assertThat(serverInfo.state).isEqualTo(ServerState.RUNNING)

        Assertions.assertThrows(CommunicatorException.NotExistServerInfo::class.java){
            serverInfoCenter.findServer("")
        }
    }


    @Test
    fun findRoundRobinServer() {

        serverInfoCenter.update(serverList)

        assertThat(serverInfoCenter.findRoundRobinServer("play").bindEndpoint).isEqualTo("tcp://127.0.0.1:0012")
        assertThat(serverInfoCenter.findRoundRobinServer("play").bindEndpoint).isEqualTo("tcp://127.0.0.1:0022")
        assertThat(serverInfoCenter.findRoundRobinServer("play").bindEndpoint).isEqualTo("tcp://127.0.0.1:0002")


        assertThat(serverInfoCenter.findRoundRobinServer("session").bindEndpoint).isEqualTo("tcp://127.0.0.1:0013")
        assertThat(serverInfoCenter.findRoundRobinServer("session").bindEndpoint).isEqualTo("tcp://127.0.0.1:0023")
        assertThat(serverInfoCenter.findRoundRobinServer("session").bindEndpoint).isEqualTo("tcp://127.0.0.1:0003")
    }

    @Test
    fun getServerList() {

        serverInfoCenter.update(serverList)
        assertThat(serverInfoCenter.getServerList().size).isEqualTo(9)
    }
}