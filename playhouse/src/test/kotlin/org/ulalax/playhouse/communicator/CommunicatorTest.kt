package org.ulalax.playhouse.communicator

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.ConsoleLogger
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.communicator.socket.ZmqJPlaySocket
import org.ulalax.playhouse.protocol.Common.HeaderMsg
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
        sessionServer = XServerCommunicator(ZmqJPlaySocket(sessionEndpoint))
        sessionClient = XClientCommunicator(ZmqJPlaySocket(sessionEndpoint))

        sessionServer.bind(object : CommunicateListener {
            override fun onReceive(routePacket: RoutePacket) {
                sessionResults.add(routePacket)
            }
        })

        apiPort = IpFinder.findFreePort()
        apiEndpoint = "tcp://${localIp}:${apiPort}"
        apiServer = XServerCommunicator(ZmqJPlaySocket(apiEndpoint))
        apiClient = XClientCommunicator(ZmqJPlaySocket(apiEndpoint))

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

            sessionClient.send(apiEndpoint, RoutePacket.clientOf("session", 0, Packet(message)))
            sleep(200)

            apiResults.size.shouldBe(1)
            apiResults[0].msgName().shouldBe("HeaderMsg")

            ////////// api to session ///////////////

            apiClient.connect(sessionEndpoint)
            sleep(100)

            apiClient.send(sessionEndpoint, RoutePacket.clientOf("api", 0, Packet("apiPacket")))
            sleep(200)

            sessionResults.size.shouldBe(1)
            sessionResults[0].msgName().shouldBe("apiPacket")
        }
    }

}
