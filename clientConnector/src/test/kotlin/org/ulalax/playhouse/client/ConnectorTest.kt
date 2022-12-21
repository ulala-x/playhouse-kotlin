package org.ulalax.playhouse.client

import org.ulalax.playhouse.protocol.Packet
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ConnectorTest {

    @BeforeEach
    fun setUp() {
    }

    @AfterEach
    fun tearDown() {
    }

    internal class ClientListenerClient: ClientPacketListener {
        override fun onReceive(serviceId: String, packet: Packet) {
            TODO("Not yet implemented")
        }
    }
    @Test
    fun connect() {
//        val connector = Connector(ClientListenerClient())
//        connector.connect("127.0.0.1",8080)
//        connector.send("api", Plcommon.HeaderMsg.newBuilder().setMsgName("HeaderMsg").build())
//        connector.send("api", Plcommon.HeaderMsg.newBuilder().setMsgName("HeaderMsg").build())
//        Thread.sleep(1000)
//        Thread.sleep(100000)
    }
}