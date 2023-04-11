package org.ulalax.playhouse.service.api.reflection.pojo.front

import kotlinx.coroutines.delay
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.protocol.Test
import org.ulalax.playhouse.service.ApiSender
import org.ulalax.playhouse.service.Sender
import org.ulalax.playhouse.service.SystemPanel
import org.ulalax.playhouse.service.api.*
import org.ulalax.playhouse.service.api.reflection.ApiReflectionTest

class TestApiService  : ApiService {
    private lateinit var systemPanel: SystemPanel
    private lateinit var sender: Sender

    override fun init(systemPanel: SystemPanel, sender: Sender) {
        this.systemPanel = systemPanel
        this.sender = sender
        ApiReflectionTest.resultMessage = "init"
    }

    override fun handles(register: HandlerRegister) {
        register.add(1,::test1)
        register.add(2,::test2)
    }

    override fun instance(): ApiService {
        return TestApiService()
    }

    fun test1(
            @Suppress("UNUSED_PARAMETER")  sessionInfo:String,
            packet: Packet,
            @Suppress("UNUSED_PARAMETER") apiSender: ApiSender){
        val message = Test.ApiTestMsg1.parseFrom(packet.data())
        ApiReflectionTest.resultMessage = message.testMsg
    }

    fun test2(
            @Suppress("UNUSED_PARAMETER") sessionInfo:String,
            packet: Packet,
            @Suppress("UNUSED_PARAMETER") apiSender: ApiSender){

//        delay(100)

        val message = Test.ApiTestMsg1.parseFrom(packet.data())
        ApiReflectionTest.resultMessage = message.testMsg
    }
}
