package org.ulalax.playhouse.communicator.socket

import org.ulalax.playhouse.communicator.CommunicateListener
import org.ulalax.playhouse.communicator.IpFinder
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.ulalax.playhouse.ConsoleLogger
import org.ulalax.playhouse.communicator.CommunicatorClient
import org.ulalax.playhouse.communicator.CommunicatorServer
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.protocol.Common.HeaderMsg
import java.lang.Thread.sleep


internal class CommunicatorTest {

    @Test
    fun communicate() {

        val localIp = IpFinder.findLocalIp()


        val sessionResults = mutableListOf<RoutePacket>()
        val sessionPort = IpFinder.findFreePort()
        val sessionEndpoint = "tcp://${localIp}:$sessionPort"
        val sessionServer = CommunicatorServer(ZmqJPlaySocket(sessionEndpoint),ConsoleLogger())
        val sessionClient = CommunicatorClient(ZmqJPlaySocket(sessionEndpoint),ConsoleLogger())

        sessionServer.bind(object : CommunicateListener {
            override fun onReceive(routePacket: RoutePacket) {
                sessionResults.add(routePacket)
            }
        })


        val apiResults = mutableListOf<RoutePacket>()
        val apiPort = IpFinder.findFreePort()
        val apiEndpoint = "tcp://${localIp}:${apiPort}"
        val apiServer = CommunicatorServer(ZmqJPlaySocket(apiEndpoint),ConsoleLogger())
        val apiClient = CommunicatorClient(ZmqJPlaySocket(apiEndpoint),ConsoleLogger())

        apiServer.bind(object : CommunicateListener {
            override fun onReceive(routePacket: RoutePacket) {
                apiResults.add(routePacket)
            }
        })
        sleep(100)

        Thread{
            sessionServer.communicate()
        }.start()
        Thread{
            sessionClient.communicate()
        }.start()
        Thread{
            apiServer.communicate()
        }.start()
        Thread{
            apiClient.communicate()
        }.start()

        ///////// session to api ///////////

        sessionClient.connect(apiEndpoint)
        sleep(100)

        val message = HeaderMsg.newBuilder().setMsgName("sessionPacket").build()

        sessionClient.send(apiEndpoint, RoutePacket.clientOf("session",0, Packet(message)))
        sleep(100)

        assertThat(apiResults.size).isEqualTo(1)
        assertThat(apiResults[0].msgName()).isEqualTo("HeaderMsg")

//        ////////// api to session ///////////////

        apiClient.connect(sessionEndpoint)
        sleep(100)

        apiClient.send(sessionEndpoint, RoutePacket.clientOf("api",0, Packet("apiPacket")))
        sleep(200)

        assertThat(sessionResults.size).isEqualTo(1)
        assertThat(sessionResults[0].msgName()).isEqualTo("apiPacket")
    }

}