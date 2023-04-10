package org.ulalax.playhouse.service.api

import org.springframework.context.ApplicationContext
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.service.ApiSender
import org.ulalax.playhouse.service.SystemPanel
import org.ulalax.playhouse.service.api.annotation.Api

typealias ApiHandler = (sessionInfo: String, packet: Packet, apiSender: ApiSender)->Unit
object ApiHandlerRegister {
    private val handlers:MutableMap<Int,ApiHandler> = mutableMapOf()
    fun add(msgId:Int,handler:ApiHandler){
        if(handlers.contains(msgId)){
            throw IllegalStateException("already exist msgId:$msgId")
        }else{
            handlers[msgId] = handler
        }
    }
}
interface ApiService {
//    val systemPanel: SystemPanel
//    val apiBaseSender: ApiBaseSender
    fun init(systemPanel: SystemPanel,apiBaseSender: ApiBaseSender)
    fun instance():ApiService
}

class GameApiService : ApiService{
    override fun init(systemPanel: SystemPanel, apiBaseSender: ApiBaseSender) {
        TODO("Not yet implemented")
    }

    override fun instance(): ApiService {
        return GameApiService()
    }
}