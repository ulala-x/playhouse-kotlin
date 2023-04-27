package org.ulalax.playhouse.service.api

import kotlin.reflect.KFunction

class XHandlerRegister : HandlerRegister {
    val handlers:MutableMap<Int,KFunction<ApiHandler>> = mutableMapOf()
    override fun add(msgId:Int,handler:ApiHandler){
        try {
            if(handlers.contains(msgId)){
                throw IllegalStateException("already exist msgId:$msgId")
            }else{
                handlers[msgId] = handler as KFunction<ApiHandler>
            }
        }catch (_:Exception){
            throw IllegalStateException("Not a suspend function in ${handler::class.java.name}")
        }

    }
}
class XBackendHandlerRegister : BackendHandlerRegister {
    val handlers:MutableMap<Int,KFunction<*>> = mutableMapOf()
    override fun add(msgId:Int, handler: ApiBackendHandler){

        try {
            if(handlers.contains(msgId)){
                throw IllegalStateException("already exist msgId:$msgId")
            }else{
                handlers[msgId] = handler as KFunction<ApiHandler>
            }
        }catch (_:Exception){
            throw IllegalStateException("Not a suspend function in ${handler::class.java.name}")
        }

    }
}