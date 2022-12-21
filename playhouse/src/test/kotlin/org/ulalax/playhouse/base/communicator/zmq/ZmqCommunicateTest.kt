package org.ulalax.playhouse.base.communicator.zmq

import org.ulalax.playhouse.base.communicator.CommunicateListener
import org.ulalax.playhouse.base.communicator.IpFinder
import org.ulalax.playhouse.base.communicator.message.RoutePacket
import org.ulalax.playhouse.base.protocol.Packet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.ulalax.playhouse.Common
import org.ulalax.playhouse.Common.HeaderMsg
import java.lang.Thread.sleep


internal class ZmqCommunicateTest {

    @Test
    fun communicate() {

        val localIp = IpFinder.findLocalIp()

        val sessionEndpoint = "tcp://${localIp}:37777"


        val sessionResults = mutableListOf<RoutePacket>()
        val sessionServer = ZmqCommunicateServer(sessionEndpoint);

        val sessionClient = sessionServer.getClient()


        val apiEndpoint = "tcp://${localIp}:38888"

        val apiResults = mutableListOf<RoutePacket>()
        val apiServer = ZmqCommunicateServer(apiEndpoint)
        val apiClient = apiServer.getClient()

        sessionServer.bind(object : CommunicateListener {
            override fun onReceive(routePacket: RoutePacket) {
                sessionResults.add(routePacket)
            }
        })

        apiServer.bind(object : CommunicateListener {
            override fun onReceive(routePacket: RoutePacket) {
                apiResults.add(routePacket)
            }
        })
//
        sleep(100)


        ///////// session to api ///////////

        sessionClient.connect(apiEndpoint)
        sessionClient.communicate()

        sleep(100)


        val message = HeaderMsg.newBuilder().setMsgName("sessionPacket").build()

        sessionClient.send(apiEndpoint,RoutePacket.clientOf("session",0, Packet(message)))
        sessionClient.communicate()
        sleep(100)
        apiServer.communicate()

        assertThat(apiResults.size).isEqualTo(1)
        assertThat(apiResults[0].msgName()).isEqualTo("HeaderMsg")
//
//        ////////// api to session ///////////////
//
        apiClient.connect(sessionEndpoint)
        apiClient.communicate()

        sleep(100)

        apiClient.send(sessionEndpoint,RoutePacket.clientOf("api",0, Packet("apiPacket")))
        apiClient.communicate()

        sleep(100)
        sessionServer.communicate()

        assertThat(sessionResults.size).isEqualTo(1)
        assertThat(sessionResults[0].msgName()).isEqualTo("apiPacket")


    }

}