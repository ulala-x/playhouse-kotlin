package org.ulalax.playhouse.service.api

import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.service.ApiBackendSender
import org.ulalax.playhouse.service.ApiSender
import org.ulalax.playhouse.service.Sender
import org.ulalax.playhouse.service.SystemPanel
import kotlin.reflect.KSuspendFunction2

typealias  ApiHandler = suspend (packet: Packet, apiSender: ApiSender)->Unit
typealias ApiBackendHandler = suspend (packet: Packet, apiSender: ApiBackendSender)->Unit


interface HandlerRegister {
    fun add(msgId:Int,handler:ApiHandler)
}
interface BackendHandlerRegister {
    fun add(msgId:Int, handler: ApiBackendHandler)
}

interface ApiService {
    suspend fun init(systemPanel: SystemPanel, sender: Sender)
    fun handles(register: HandlerRegister)
    fun instance():ApiService
}

interface ApiBackendService {
    suspend fun init(systemPanel: SystemPanel,sender: Sender)
    fun handles(register: BackendHandlerRegister)
    fun instance():ApiBackendService
}


