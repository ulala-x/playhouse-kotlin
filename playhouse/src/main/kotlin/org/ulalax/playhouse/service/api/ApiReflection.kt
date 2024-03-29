package org.ulalax.playhouse.service.api

import org.ulalax.playhouse.communicator.message.RouteHeader
import org.apache.commons.lang3.exception.ExceptionUtils
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import LOG
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.protocol.Common.BaseErrorCode
import org.ulalax.playhouse.service.ApiBackendSender
import org.ulalax.playhouse.service.ApiSender
import org.ulalax.playhouse.service.Sender
import org.ulalax.playhouse.service.SystemPanel
import java.lang.reflect.Method
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinFunction
import kotlin.system.exitProcess
import kotlin.coroutines.Continuation

data class ApiMethod(val msgId:Int,val className: String,val method: Method)
data class ApiInstance(val instance:Any)


class ApiReflection(packageName: String) {

    val instances: MutableMap<String, ApiInstance> = mutableMapOf()
    private val initMethods:MutableList<ApiMethod> = mutableListOf()
    private val methods:MutableMap<Int, ApiMethod> = HashMap()
    private val backendMethods:MutableMap<Int, ApiMethod> = HashMap()
    private val messageIndexChecker:MutableMap<Int,String> =  mutableMapOf()
    private val backendMessageIndexChecker:MutableMap<Int,String> =  mutableMapOf()

    init {
        val reflections = Reflections(packageName, Scanners.SubTypes,Scanners.MethodsSignature)


        extractInstance(reflections)
        extractHandlerMethod(reflections)
    }

    suspend fun callInitMethod(systemPanel: SystemPanel, sender: Sender){
        initMethods.forEach{targetMethod->

            try{
                if(!instances.containsKey(targetMethod.className)) throw ApiException.NotRegisterApiInstance(
                    targetMethod.className
                )

                val apiInstance:ApiInstance = instances[targetMethod.className]!!



                targetMethod.method.kotlinFunction!!.callSuspend(apiInstance.instance,systemPanel,sender)
                //targetMethod.method.invoke(apiInstance.instance,systemPanel,sender)


            }catch (e:Exception){
                LOG.error(ExceptionUtils.getStackTrace(e),this,e)
                exitProcess(1)
            }
        }
    }

    suspend fun callMethod(routeHeader: RouteHeader, packet: Packet, isBackend :Boolean, apiSender: AllApiSender) = packet.use{
        val msgId = routeHeader.msgId()
        //val packet = Packet(msgName,routePacket.movePayload())
        //val isBackend = routePacket.isBackend()

        val targetMethod:ApiMethod

        if(isBackend) {
            if(!backendMethods.containsKey(msgId)){
                LOG.error("msgId:${packet.msgId} is not registered, backend from:${routeHeader.from}",this)
                apiSender.errorReply(routeHeader, BaseErrorCode.UNREGISTER_PACKET_VALUE.toShort())
                return@use
            }
            targetMethod =  backendMethods[msgId]!!
        } else{
            if(!methods.containsKey(msgId)) {
                LOG.error("msgId:${packet.msgId} is not registered, from:${routeHeader.from}",this)
                apiSender.errorReply(routeHeader, BaseErrorCode.UNREGISTER_PACKET_VALUE.toShort())
                return@use
                //throw ApiException.NotRegisterApiMethod("msgId:${packet.msgId} is not registered, from:${routeHeader.from}")
            }
            targetMethod =  methods[msgId]!!
        }

        if(!instances.containsKey(targetMethod.className)) throw ApiException.NotRegisterApiInstance(targetMethod.className)
        val targetInstance = instances[targetMethod.className]!!

        try {
            if(isBackend){
                targetMethod.method.kotlinFunction!!.callSuspend(targetInstance.instance,packet,apiSender as ApiBackendSender)
//                targetMethod.method.invoke(targetInstance.instance,packet,apiSender as ApiBackendSender)
            }else{
                targetMethod.method.kotlinFunction!!.callSuspend(targetInstance.instance,packet,apiSender as ApiSender)
//                targetMethod.method.invoke(targetInstance.instance,packet,apiSender as ApiSender)

            }
        }catch (e:Exception){
            apiSender.errorReply(routeHeader, BaseErrorCode.UNCHECKED_CONTENTS_ERROR_VALUE.toShort())
            LOG.error(ExceptionUtils.getStackTrace(e),this,e)
        }

    }

    private fun extractHandlerMethod(reflections: Reflections) {
        registerInitMethod(reflections)
        registerHandlerMethod(reflections)
        //registerBackendHandlerMethod(reflections)
    }


    private fun registerInitMethod(reflections: Reflections){

        reflections.getMethodsWithSignature(SystemPanel::class.java,Sender::class.java,Continuation::class.java)
                .filter { el->el.name == "init" && !el.declaringClass.isInterface}
                .forEach{ method ->
            initMethods.add(ApiMethod(0, method.declaringClass.name, method))
        }

    }
    private fun registerHandlerMethod(reflections: Reflections){
        val handleMethods = reflections.getMethodsWithSignature(HandlerRegister::class.java,BackendHandlerRegister::class.java).filter { el->el.name=="handles" }
        handleMethods.forEach { method ->
            if(!method.declaringClass.isInterface){
                val className = method.declaringClass.name
                val apiInstance:ApiInstance = instances[className]!!
                val handlerRegister = XHandlerRegister()
                val backendHandlerRegister = XBackendHandlerRegister()
                method.invoke(apiInstance.instance,handlerRegister,backendHandlerRegister)

                handlerRegister.handlers.forEach { el ->
                    if(messageIndexChecker.contains(el.key)){
                        throw ApiException.DuplicatedMessageIndex("registered msgId is duplicated - msgId:${el.key}, methods: ${messageIndexChecker[el.key]}, ${el.value.name}")
                    }
                    this.methods[el.key] = ApiMethod(el.key,className,el.value.javaMethod!!)
                    messageIndexChecker[el.key] = el.value.name
                }

                backendHandlerRegister.handlers.forEach { el ->
                    if (backendMessageIndexChecker.contains(el.key)) {
                        throw ApiException.DuplicatedMessageIndex("registered msgId is duplicated - msgId:${el.key}, methods: ${messageIndexChecker[el.key]}, ${el.value.name}")
                    }
                    this.backendMethods[el.key] = ApiMethod(el.key, className, el.value.javaMethod!!)
                    backendMessageIndexChecker[el.key] = el.value.name
                }
            }
        }
    }
//    private fun registerBackendHandlerMethod(reflections: Reflections){
//        val handleMethods = reflections.getMethodsWithSignature(BackendHandlerRegister::class.java).filter { el->el.name=="handles" }
//        handleMethods.forEach { method ->
//            if(!method.declaringClass.isInterface) {
//                val className = method.declaringClass.name
//                val apiInstance: ApiInstance = instances[className]!!
//                val handlerRegister = XBackendHandlerRegister()
//                method.invoke(apiInstance.instance, handlerRegister)
//                handlerRegister.handlers.forEach { el ->
//                    if (messageIndexChecker.contains(el.key)) {
//                        throw ApiException.DuplicatedMessageIndex("registered msgId is duplicated - msgId:${el.key}, methods: ${messageIndexChecker[el.key]}, ${el.value.name}")
//                    }
//                    this.backendMethods[el.key] = ApiMethod(el.key, className, el.value.javaMethod!!)
//                    messageIndexChecker[el.key] = el.value.name
//                }
//            }
//        }
//    }


    private fun extractInstance(reflections: Reflections) {
        val classes = reflections.getSubTypesOf(ApiService::class.java)
        classes.forEach { clazz ->
            val name = clazz.name
            val instanceMethod = clazz.getMethod("instance")
            val instance = instanceMethod.invoke(clazz.getDeclaredConstructor().newInstance()) as ApiService
            instances[name] = ApiInstance(instance)
        }
//        val backendClasses = reflections.getSubTypesOf(ApiBackendService::class.java)
//        backendClasses.forEach { clazz ->
//            val name = clazz.name
//            val instanceMethod = clazz.getMethod("instance")
//            val instance = instanceMethod.invoke(clazz.getDeclaredConstructor().newInstance()) as ApiBackendService
//            instances[name] = ApiInstance(instance)
//        }
    }



}