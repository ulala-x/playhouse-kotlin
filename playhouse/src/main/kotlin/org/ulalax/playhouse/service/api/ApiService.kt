package org.ulalax.playhouse.service.api

import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.service.ApiBackendSender
import org.ulalax.playhouse.service.ApiSender
import org.ulalax.playhouse.service.Sender
import org.ulalax.playhouse.service.SystemPanel

typealias  ApiHandler = (sessionInfo: String, packet: Packet, apiSender: ApiSender)->Unit
typealias ApiBackendHandler = (sessionInfo: String, packet: Packet, apiSender: ApiBackendSender)->Unit


interface HandlerRegister {
    fun add(msgId:Int,handler:ApiHandler)
}
interface BackendHandlerRegister {
    fun add(msgId:Int,handler:ApiBackendHandler)
}

interface ApiService {
    fun init(systemPanel: SystemPanel, sender: Sender)
    fun handles(register: HandlerRegister)
    fun instance():ApiService
}

interface ApiBackendService {
    fun init(systemPanel: SystemPanel,sender: Sender)
    fun handles(register: BackendHandlerRegister)
    fun instance():ApiBackendService
}


