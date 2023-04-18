package org.ulalax.playhouse.service.api.pojo.back

import kotlinx.coroutines.delay
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.protocol.Test
import org.ulalax.playhouse.service.ApiBackendSender
import org.ulalax.playhouse.service.Sender
import org.ulalax.playhouse.service.SystemPanel
import org.ulalax.playhouse.service.api.ApiBackendService
import org.ulalax.playhouse.service.api.BackendHandlerRegister
import org.ulalax.playhouse.service.api.ApiReflectionTest


class TestApiBackendService : ApiBackendService {
    private lateinit var systemPanel: SystemPanel
    private lateinit var sender: Sender

    override suspend fun init(systemPanel: SystemPanel, sender: Sender) {
        this.systemPanel = systemPanel
        this.sender = sender
        ApiReflectionTest.resultMessage = "backend init"
    }

    override fun handles(@Suppress("UNUSED_PARAMETER")register: BackendHandlerRegister) {
        register.add(3,::test1)
        register.add(4,::test2)
    }

    override fun instance(): ApiBackendService {
        return TestApiBackendService()
    }
    suspend fun test1(
            packet: Packet,
            @Suppress("UNUSED_PARAMETER") apiBackendSender: ApiBackendSender){
        val message = Test.ApiTestMsg1.parseFrom(packet.data())
        ApiReflectionTest.resultMessage = message.testMsg
        delay(10)
    }

    suspend fun test2(
            packet: Packet,
            @Suppress("UNUSED_PARAMETER") apiBackendSender: ApiBackendSender){
        val message = Test.ApiTestMsg1.parseFrom(packet.data())
        ApiReflectionTest.resultMessage = message.testMsg
        delay(10)
    }

}