package org.ulalax.playhouse.service.api.beans

import kotlinx.coroutines.delay
import org.springframework.stereotype.Component
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.protocol.Test
import org.ulalax.playhouse.service.ApiBackendSender
import org.ulalax.playhouse.service.ApiSender
import org.ulalax.playhouse.service.Sender
import org.ulalax.playhouse.service.SystemPanel
import org.ulalax.playhouse.service.api.*
import org.ulalax.playhouse.service.api.ApiReflectionTest
import org.ulalax.playhouse.service.api.AppContext

@Component
open class TestApiServiceSpringBeans : ApiService {
    private lateinit var systemPanel: SystemPanel
    private lateinit var sender: Sender

    override suspend fun init(systemPanel: SystemPanel, sender: Sender) {
        this.systemPanel = systemPanel
        this.sender = sender
        ApiReflectionTest.resultMessage = "SpringBeanInit"
    }

    override fun handles(register: HandlerRegister,backendHandlerRegister: BackendHandlerRegister) {
        register.add(Test.ApiTestBeanMsg.getDescriptor().index,::test1)
        backendHandlerRegister.add(Test.ApiBackendTestBeanMsg.getDescriptor().index,::test2)
    }

    override fun instance(): ApiService {
        return AppContext.applicationContext.getBean(this::class.java) as TestApiServiceSpringBeans
    }

    suspend fun test1(
            packet: Packet,
            @Suppress("UNUSED_PARAMETER") apiSender: ApiSender){
        val message = Test.ApiTestBeanMsg.parseFrom(packet.data())
        ApiReflectionTest.resultMessage = message.testMsg
        delay(1)
    }

    suspend fun test2(
        @Suppress("UNUSED_PARAMETER")packet: Packet,
        @Suppress("UNUSED_PARAMETER")apiSender: ApiBackendSender
    ) {
        val message = Test.ApiBackendTestBeanMsg.parseFrom(packet.data())
        ApiReflectionTest.resultMessage = message.testMsg
        delay(10)
    }

}
