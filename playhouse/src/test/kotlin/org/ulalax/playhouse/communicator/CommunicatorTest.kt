package org.ulalax.playhouse.communicator

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.communicator.socket.SocketConfig
import org.ulalax.playhouse.communicator.socket.ZmqJPlaySocket
import org.ulalax.playhouse.protocol.Server
import org.ulalax.playhouse.protocol.Server.HeaderMsg
import java.lang.Thread.sleep


internal class CommunicatorTest : FunSpec() {

    private var localIp = IpFinder.findLocalIp()
    private var sessionPort: Int = 0
    private var sessionEndpoint: String
    private var sessionServer: XServerCommunicator
    private var sessionClient: XClientCommunicator
    var sessionResults = mutableListOf<RoutePacket>()

    private var apiPort: Int = 0
    private var apiEndpoint: String
    private var apiServer: XServerCommunicator
    private var apiClient: XClientCommunicator
    var apiResults = mutableListOf<RoutePacket>()

    init {

        sessionPort = IpFinder.findFreePort()
        sessionEndpoint = "tcp://${localIp}:$sessionPort"
        sessionServer = XServerCommunicator(ZmqJPlaySocket(SocketConfig(),sessionEndpoint))
        sessionClient = XClientCommunicator(ZmqJPlaySocket(SocketConfig(),sessionEndpoint))

        sessionServer.bind(object : CommunicateListener {
            override fun onReceive(routePacket: RoutePacket) {
                sessionResults.add(routePacket)
            }
        })

        apiPort = IpFinder.findFreePort()
        apiEndpoint = "tcp://${localIp}:${apiPort}"
        apiServer = XServerCommunicator(ZmqJPlaySocket(SocketConfig(),apiEndpoint))
        apiClient = XClientCommunicator(ZmqJPlaySocket(SocketConfig(),apiEndpoint))

        apiServer.bind(object : CommunicateListener {
            override fun onReceive(routePacket: RoutePacket) {
                apiResults.add(routePacket)
            }
        })

        test("should communicate between session and api")
        {
            Thread {
                sessionServer.communicate()
            }.start()
            Thread {
                sessionClient.communicate()
            }.start()
            Thread {
                apiServer.communicate()
            }.start()
            Thread {
                apiClient.communicate()
            }.start()

            ////////// session to api ///////////

            sessionClient.connect(apiEndpoint)
            sleep(100)

            val message = HeaderMsg.newBuilder().build()

            sessionClient.send(apiEndpoint, RoutePacket.clientOf(1, 0, Packet(message)))
            sleep(200)

            apiResults.size.shouldBe(1)
            apiResults[0].msgId.shouldBe(HeaderMsg.getDescriptor().index)

            ////////// api to session ///////////////

            apiClient.connect(sessionEndpoint)
            sleep(100)

            apiClient.send(sessionEndpoint, RoutePacket.clientOf(2, 0, Packet(1)))
            sleep(200)

            sessionResults.size.shouldBe(1)
            sessionResults[0].msgId.shouldBe(1)
        }
    }

}
