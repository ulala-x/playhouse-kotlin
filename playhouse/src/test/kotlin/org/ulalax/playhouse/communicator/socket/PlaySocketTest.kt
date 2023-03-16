package org.ulalax.playhouse.communicator.socket

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.ulalax.playhouse.protocol.Common.HeaderMsg
import org.ulalax.playhouse.communicator.message.RouteHeader
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.protocol.Test.TestMsg
import org.ulalax.playhouse.communicator.IpFinder
import org.ulalax.playhouse.communicator.message.EmptyPayload

class PlaySocketTest : FunSpec(){
    private val localIp = IpFinder.findLocalIp()
    private val serverPort = 18888
    private val clientPort = 17777

    private val serverBindEndpoint = "tcp://${localIp}:$serverPort"
    private val clientBindEndpoint = "tcp://${localIp}:$clientPort"

    private lateinit var serverSocket: ZmqJPlaySocket
    private lateinit var clientSocket: ZmqJPlaySocket

    init {
        beforeTest {
            serverSocket = ZmqJPlaySocket(serverBindEndpoint)
            serverSocket.bind()

            clientSocket = ZmqJPlaySocket(clientBindEndpoint)
            clientSocket.bind()

            Thread.sleep(100)
            clientSocket.connect(serverBindEndpoint)

            Thread.sleep(100)
        }

        afterTest {
            serverSocket.close()
            clientSocket.close()
        }


        test("should send message with empty frame") {
            val sendRoutePacket = RoutePacket.of(RouteHeader.of(HeaderMsg.newBuilder().build()), EmptyPayload())
            clientSocket.send(serverBindEndpoint, sendRoutePacket)

            var receiveRoutePacket: RoutePacket? = null
            while (receiveRoutePacket == null) {
                receiveRoutePacket = serverSocket.receive()
                Thread.sleep(10)
            }

            receiveRoutePacket.data().size.shouldBe(0)
        }

        test("should send and receive a message") {
            val message = TestMsg.newBuilder().setTestMsg("hello").setTestNumber(27).build()

            val header = HeaderMsg.newBuilder()
                    .setErrorCode(0)
                    .setMsgSeq(1)
                    .setServiceId("session")
                    .setMsgName("TestMsg")
                    .build()

            val routeHeader = RouteHeader.of(header)

            val sendRoutePacket = RoutePacket.of(routeHeader, message)

            clientSocket.send(serverBindEndpoint, sendRoutePacket)

            var receiveRoutePacket: RoutePacket? = null
            while (receiveRoutePacket == null) {
                receiveRoutePacket = serverSocket.receive()
                Thread.sleep(10)
            }

            receiveRoutePacket!!.routeHeader.header.toMsg().shouldBe(header)
            receiveRoutePacket!!.routeHeader.from.shouldBe(clientBindEndpoint)

            val receiveBody = TestMsg.parseFrom(receiveRoutePacket!!.data())

            receiveBody.shouldBe(message)
        }
    }


}