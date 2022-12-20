package com.lifemmo.pl.base.communicator.zmq

import com.lifemmo.pl.base.ByteBufferAllocator
import com.lifemmo.pl.base.PlTest.TestMsg
import com.lifemmo.pl.base.Plbase
import com.lifemmo.pl.base.Plbase.RoutePacketMsg
import com.lifemmo.pl.base.Plcommon.HeaderMsg
import com.lifemmo.pl.base.communicator.*
import com.lifemmo.pl.base.communicator.message.RouteHeader
import com.lifemmo.pl.base.communicator.message.RoutePacket
import com.lifemmo.pl.base.protocol.ProtoPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ZmqSocketTest {

    val localIp = IpFinder.findLocalIp()
    val serverPort = 18888
    val clientPort = 17777

    val serverBindEndpoint = "tcp://${localIp}:$serverPort"
    val clientBindEndpoint = "tcp://${localIp}:$clientPort"
//    val serverBindEndpoint = "ws://${localIp}:$serverPort"
//    val clientBindEndpoint = "ws://${localIp}:$clientPort"

    private lateinit var serverSocket:ZmqSocket
    private lateinit var clientSocket:ZmqSocket

    @BeforeEach
    fun setUp() {
        serverSocket = JZmqSocket(serverBindEndpoint)
        serverSocket.bind()


        clientSocket = JZmqSocket(clientBindEndpoint)
        clientSocket.bind()

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
        val sendRoutePacket = RoutePacket.of(RouteHeader.of(HeaderMsg.newBuilder().build()),ProtoPayload())
        clientSocket.send(serverBindEndpoint, sendRoutePacket)
        var receiveRoutePacket:RoutePacket? = null
        while(receiveRoutePacket==null){
            receiveRoutePacket = serverSocket.receive()
            Thread.sleep(10)
        }

    }

    @Test
    fun send()  {

        val message = TestMsg.newBuilder().setTestMsg("hello").setTestNumber(27).build()

        val header = HeaderMsg.newBuilder().setBaseErrorCode(0).setMsgSeq(1).setServiceId("session").setMsgName("TestMsg").build()

//        val routePacket = RoutePacketMsg.newBuilder()
//            .setRouteHeaderMsg(Plbase.RouteHeaderMsg.newBuilder().setHeaderMsg(header)).setMessage(message.toByteString()).build()
//
        val routeHeader = RouteHeader.of(header)

        val sendRoutePacket = RoutePacket.of(routeHeader,message)

        clientSocket.send(serverBindEndpoint, sendRoutePacket)

        var receiveRoutePacket:RoutePacket? = null
        while(receiveRoutePacket==null){
            receiveRoutePacket = serverSocket.receive()
            Thread.sleep(10)
        }

        assertThat(receiveRoutePacket.routeHeader.header.toMsg()).isEqualTo(header)
        assertThat(receiveRoutePacket.routeHeader.from).isEqualTo(clientBindEndpoint)

        val receiveBody = TestMsg.parseFrom(receiveRoutePacket.buffer())

        assertThat(receiveBody).isEqualTo(message)


        serverSocket.close()
        clientSocket.close()

    }
}