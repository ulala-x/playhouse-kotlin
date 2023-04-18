package org.ulalax.playhouse.service.api.pojo.front

import kotlinx.coroutines.delay
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.protocol.Test
import org.ulalax.playhouse.service.ApiSender
import org.ulalax.playhouse.service.Sender
import org.ulalax.playhouse.service.SystemPanel
import org.ulalax.playhouse.service.api.*
import org.ulalax.playhouse.service.api.ApiReflectionTest

class TestApiService  : ApiService {
    private lateinit var systemPanel: SystemPanel
    private lateinit var sender: Sender

    override suspend fun init(systemPanel: SystemPanel, sender: Sender) {
        this.systemPanel = systemPanel
        this.sender = sender
        ApiReflectionTest.resultMessage = "init"
        delay(1)
    }

    override fun handles(register: HandlerRegister) {
        register.add(1,::test1)
        register.add(2,::test2)
    }

    override fun instance(): ApiService {
        return TestApiService()
    }

    suspend fun test1(
            packet: Packet,
            @Suppress("UNUSED_PARAMETER") apiSender: ApiSender){
        val message = Test.ApiTestMsg1.parseFrom(packet.data())
        ApiReflectionTest.resultMessage = message.testMsg
        delay(10)
    }

    suspend fun test2(
            packet: Packet,
            @Suppress("UNUSED_PARAMETER") apiSender: ApiSender){

        delay(10)

        val message = Test.ApiTestMsg1.parseFrom(packet.data())
        ApiReflectionTest.resultMessage = message.testMsg
    }
}
