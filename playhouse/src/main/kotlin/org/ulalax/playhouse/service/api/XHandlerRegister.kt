package org.ulalax.playhouse.service.api

import kotlin.reflect.KFunction

class XHandlerRegister : HandlerRegister {
    val handlers:MutableMap<Int,KFunction<*>> = mutableMapOf()
    override fun add(msgId:Int,handler:ApiHandler){
        if(handlers.contains(msgId)){
            throw IllegalStateException("already exist msgId:$msgId")
        }else{
            handlers[msgId] = handler as KFunction<*>
        }
    }
}
class XBackendHandlerRegister : BackendHandlerRegister {
    val handlers:MutableMap<Int,KFunction<*>> = mutableMapOf()
    override fun add(msgId:Int,handler:ApiBackendHandler){
        if(handlers.contains(msgId)){
            throw IllegalStateException("already exist msgId:$msgId")
        }else{
            handlers[msgId] = handler as KFunction<*>
        }
    }
}