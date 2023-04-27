package org.ulalax.playhouse.service.api.pojo

import kotlinx.coroutines.delay
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.protocol.Test
import org.ulalax.playhouse.service.ApiBackendSender
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

    override fun handles(register: HandlerRegister, backendRegister: BackendHandlerRegister) {
        register.add(Test.ApiTestMsg.getDescriptor().index,::test1)
        backendRegister.add(Test.ApiBackendTestMsg.getDescriptor().index,::test2)
    }

    override fun instance(): ApiService {
        return TestApiService()
    }

    suspend fun test1(
        packet: Packet,
        @Suppress("UNUSED_PARAMETER") apiSender: ApiSender){
        val message = Test.ApiTestMsg.parseFrom(packet.data())
        ApiReflectionTest.resultMessage = message.testMsg
      //  delay(1)
    }

    suspend fun test2(
        @Suppress("UNUSED_PARAMETER")packet: Packet,
        @Suppress("UNUSED_PARAMETER")apiSender: ApiBackendSender
    ) {
        val message = Test.ApiBackendTestMsg.parseFrom(packet.data())
        ApiReflectionTest.resultMessage = message.testMsg
        delay(10)
    }
}
