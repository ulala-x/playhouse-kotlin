package org.ulalax.playhouse.communicator.socket

import org.ulalax.playhouse.protocol.Common.HeaderMsg
import org.ulalax.playhouse.communicator.message.RouteHeader
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.ulalax.playhouse.protocol.Test.TestMsg
import org.ulalax.playhouse.communicator.IpFinder
import org.ulalax.playhouse.communicator.message.BasePayload

internal class IPlaySocketTest {

    val localIp = IpFinder.findLocalIp()
    val serverPort = 18888
    val clientPort = 17777

    val serverBindEndpoint = "tcp://${localIp}:$serverPort"
    val clientBindEndpoint = "tcp://${localIp}:$clientPort"

    private lateinit var serverSocket: ZmqJPlaySocket
    private lateinit var clientSocket: ZmqJPlaySocket

    @BeforeEach
    fun setUp() {
        serverSocket = ZmqJPlaySocket(serverBindEndpoint)
        serverSocket.bind()


        clientSocket = ZmqJPlaySocket(clientBindEndpoint)
        clientSocket.bind()

        Thread.sleep(100)
        clientSocket.connect(serverBindEndpoint)

        Thread.sleep(100)
    }

    @AfterEach
    fun tearDown() {
        serverSocket.close()
        clientSocket.close()
    }

    @Test
    fun bufferRefCnt(){

    }

    @Test
    fun send_with_empty_frame(){
        val sendRoutePacket = RoutePacket.of(RouteHeader.of(HeaderMsg.newBuilder().build()), BasePayload())
        clientSocket.send(serverBindEndpoint, sendRoutePacket)

        var receiveRoutePacket: RoutePacket? = null
        while(receiveRoutePacket==null){
            receiveRoutePacket = serverSocket.receive()
            Thread.sleep(10)
        }

    }

    @Test
    fun send()  {

        val message = TestMsg.newBuilder().setTestMsg("hello").setTestNumber(27).build()

        val header = HeaderMsg.newBuilder().setErrorCode(0).setMsgSeq(1).setServiceId("session").setMsgName("TestMsg").build()

//        val routePacket = RoutePacketMsg.newBuilder()
//            .setRouteHeaderMsg(Plbase.RouteHeaderMsg.newBuilder().setHeaderMsg(header)).setMessage(message.toByteString()).build()
//
        val routeHeader = RouteHeader.of(header)

        val sendRoutePacket = RoutePacket.of(routeHeader,message)

        clientSocket.send(serverBindEndpoint, sendRoutePacket)

        var receiveRoutePacket: RoutePacket? = null
        while(receiveRoutePacket==null){
            receiveRoutePacket = serverSocket.receive()
            Thread.sleep(10)
        }

        assertThat(receiveRoutePacket.routeHeader.header.toMsg()).isEqualTo(header)
        assertThat(receiveRoutePacket.routeHeader.from).isEqualTo(clientBindEndpoint)

        val receiveBody = TestMsg.parseFrom(receiveRoutePacket.data())

        assertThat(receiveBody).isEqualTo(message)


        serverSocket.close()
        clientSocket.close()

    }
}