package org.ulalax.playhouse.service.api.reflection.beans.back

import com.google.protobuf.duration
import io.kotest.common.runBlocking
import kotlinx.coroutines.*
import org.springframework.stereotype.Component
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.protocol.Test
import org.ulalax.playhouse.service.ApiBackendSender
import org.ulalax.playhouse.service.Sender
import org.ulalax.playhouse.service.SystemPanel
import org.ulalax.playhouse.service.api.ApiBackendService
import org.ulalax.playhouse.service.api.BackendHandlerRegister
import org.ulalax.playhouse.service.api.reflection.ApiReflectionTest
import org.ulalax.playhouse.service.api.reflection.AppContext
import java.lang.Thread.sleep


@Component
open class TestApiBackendServiceSpringBeans : ApiBackendService {

    private lateinit var systemPanel: SystemPanel
    private lateinit var sender: Sender
    override fun init(systemPanel: SystemPanel, sender: Sender) {
        this.systemPanel = systemPanel
        this.sender = sender
        ApiReflectionTest.resultMessage = "backend SpringBeanInit"
    }

    override fun handles(register: BackendHandlerRegister) {
        register.add(13,::test1)
        register.add(14,::test2)

    }

    override fun instance(): ApiBackendService {
        return AppContext.applicationContext.getBean(this::class.java) as TestApiBackendServiceSpringBeans
    }

    fun test1(
            @Suppress("UNUSED_PARAMETER")sessionInfo:String,
            @Suppress("UNUSED_PARAMETER")packet: Packet,
            @Suppress("UNUSED_PARAMETER")apiSender: ApiBackendSender){
        val message = Test.ApiTestMsg1.parseFrom(packet.data())
        ApiReflectionTest.resultMessage = message.testMsg
    }
    fun test2(
            @Suppress("UNUSED_PARAMETER")sessionInfo:String,
            @Suppress("UNUSED_PARAMETER")packet: Packet,
            @Suppress("UNUSED_PARAMETER")apiSender: ApiBackendSender) {


        val message = Test.ApiTestMsg1.parseFrom(packet.data())
        ApiReflectionTest.resultMessage = message.testMsg
    }
}
