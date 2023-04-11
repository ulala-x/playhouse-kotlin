package org.ulalax.playhouse.service.api.reflection.beans.front

import org.springframework.stereotype.Component
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.protocol.Test
import org.ulalax.playhouse.service.ApiSender
import org.ulalax.playhouse.service.Sender
import org.ulalax.playhouse.service.SystemPanel
import org.ulalax.playhouse.service.api.*
import org.ulalax.playhouse.service.api.reflection.ApiReflectionTest
import org.ulalax.playhouse.service.api.reflection.AppContext

@Component
open class TestApiServiceSpringBeans : ApiService {
    private lateinit var systemPanel: SystemPanel
    private lateinit var sender: Sender

    override fun init(systemPanel: SystemPanel, sender: Sender) {
        this.systemPanel = systemPanel
        this.sender = sender
        ApiReflectionTest.resultMessage = "SpringBeanInit"
    }

    override fun handles(register: HandlerRegister) {
        register.add(11,::test1)
        register.add(12,::test2)
    }

    override fun instance(): ApiService {
        return AppContext.applicationContext.getBean(this::class.java) as TestApiServiceSpringBeans
    }

    fun test1(
            @Suppress("UNUSED_PARAMETER") sessionInfo:String,
            packet: Packet,
            @Suppress("UNUSED_PARAMETER") apiSender: ApiSender){
        val message = Test.ApiTestMsg1.parseFrom(packet.data())
        ApiReflectionTest.resultMessage = message.testMsg
    }

    fun test2(
            @Suppress("UNUSED_PARAMETER") sessionInfo:String,
            packet: Packet,
            @Suppress("UNUSED_PARAMETER") apiSender: ApiSender){
        val message = Test.ApiTestMsg1.parseFrom(packet.data())
        ApiReflectionTest.resultMessage = message.testMsg
    }

}
