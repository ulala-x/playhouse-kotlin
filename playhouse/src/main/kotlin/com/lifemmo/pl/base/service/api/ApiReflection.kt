package com.lifemmo.pl.base.service.api

import com.lifemmo.pl.base.BaseErrorCode
import com.lifemmo.pl.base.communicator.message.RouteHeader
import com.lifemmo.pl.base.communicator.message.RoutePacket
import com.lifemmo.pl.base.protocol.Packet
import com.lifemmo.pl.base.service.*
import com.lifemmo.pl.base.service.api.annotation.Api
import com.lifemmo.pl.base.service.api.annotation.ApiBackendHandler
import com.lifemmo.pl.base.service.api.annotation.ApiHandler
import com.lifemmo.pl.base.service.api.annotation.Init
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.logging.log4j.kotlin.logger
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.lang.reflect.Method
import java.security.InvalidParameterException
import kotlin.system.exitProcess

data class ApiMethod(val msgName:String,val className: String,val method: Method)
data class ApiInstance(val instance:Any)


class ApiReflection(packageName: String, private val applicationContext: ApplicationContext) {
    val log = logger()
    val instances: MutableMap<String,ApiInstance> = HashMap()
    val initMethods:MutableList<ApiMethod> = mutableListOf()
    val methods:MutableMap<String,ApiMethod> = HashMap()
    val backendMethods:MutableMap<String,ApiMethod> = HashMap()

    init {
        val reflections = Reflections(packageName, Scanners.MethodsAnnotated, Scanners.TypesAnnotated)
        extractInstance(reflections)
        extractHandlerMethod(reflections)
    }

    fun callInitMethod(systemPanel: SystemPanel,apiBaseSender: ApiBaseSender){
        initMethods.forEach{targetMethod->

            try{
                if(!instances.containsKey(targetMethod.className)) throw ApiException.NotRegisterApiInstance(targetMethod.className)
                val targetInstance = instances[targetMethod.className]!!
                targetMethod.method.invoke(targetInstance.instance,systemPanel,apiBaseSender)
            }catch (e:Exception){
                log.error(ExceptionUtils.getStackTrace(e))
                exitProcess(1)
            }
        }
    }
    fun callMethod(routeHeader: RouteHeader,packet: Packet,isBackend :Boolean, apiSender: ApiSenderImpl) = packet.use{
        val msgName = routeHeader.msgName()
        val sessionInfo = routeHeader.sessionInfo
        //val packet = Packet(msgName,routePacket.movePayload())
        //val isBackend = routePacket.isBackend()

        val targetMethod = if(isBackend) {
            if(!backendMethods.containsKey(msgName)) throw ApiException.NotRegisterApiMethod(msgName)
            backendMethods[msgName]!!
        } else{
            if(!methods.containsKey(msgName)) throw ApiException.NotRegisterApiMethod(msgName)
            methods[msgName]!!
        }

        if(!instances.containsKey(targetMethod.className)) throw ApiException.NotRegisterApiInstance(targetMethod.className)
        val targetInstance = instances[targetMethod.className]!!

        try {
            if(isBackend){
                targetMethod.method.invoke(targetInstance.instance,sessionInfo,packet,apiSender as ApiBackendSender)
            }else{
                targetMethod.method.invoke(targetInstance.instance,sessionInfo,packet,apiSender as ApiSender)
            }
        }catch (e:Exception){
            apiSender.errorReply(routeHeader,BaseErrorCode.UNCHECKED_CONTENTS_ERROR)
            log.error(ExceptionUtils.getStackTrace(e))
        }
    }

    private fun extractHandlerMethod(reflections: Reflections) {

        val initAnnotation = reflections.getMethodsAnnotatedWith(Init::class.java)
        extractInitMethod(initAnnotation,initMethods)

        val methodsAnnotation = reflections.getMethodsAnnotatedWith(ApiHandler::class.java)
        extractHandlerMethod(methodsAnnotation,methods)

        val backendMethodsAnnotation = reflections.getMethodsAnnotatedWith(ApiBackendHandler::class.java)
        extractBackendMethod(backendMethodsAnnotation,backendMethods)
    }


    private fun extractInitMethod(methods: Set<Method>, methodList:MutableList<ApiMethod>){
        methods.forEach { method ->
            if (method.parameterCount != 2) throw InvalidParameterException("${method.declaringClass.name} : invalid Api init Handler method parameter count ${method.parameterCount}, init method has 2 parameter")
            val parameterTypes = method.parameterTypes
            if (parameterTypes[0] != SystemPanel::class.java) throw InvalidParameterException("${method.declaringClass.name} : 1st parameter type is not SystemPanel but ${parameterTypes[0]}")
            if (parameterTypes[1] != ApiBaseSender::class.java) throw InvalidParameterException("${method.declaringClass.name} : 2st parameter type is not ApiBaseSender but ${parameterTypes[1]}")

//            if (parameterTypes[2] != ApiSender::class.java) throw InvalidParameterException("3nd parameter type is not ApiSender but ${parameterTypes[2]}")
//            val apiHandler = method.getAnnotation(Init::class.java) as Init
            //if (methodMap.containsKey(apiHandler.msgName)) throw ApiException.DuplicateApiHandler(apiHandler.msgName)
            methodList.add(ApiMethod("", method.declaringClass.name, method))
        }
    }
    private fun extractHandlerMethod(methods: Set<Method>, methodMap:MutableMap<String,ApiMethod>){
        methods.forEach { method ->
            if (method.parameterCount != 3) throw InvalidParameterException("${method.declaringClass.name} : invalid ApiHandler method parameter count ${method.parameterCount}, ApiHandler method has 3 parameters")
            val parameterTypes = method.parameterTypes
            if (parameterTypes[0] != String::class.java) throw InvalidParameterException("${method.declaringClass.name} : 1st parameter type is not String but ${parameterTypes[0]}")
            if (parameterTypes[1] != Packet::class.java) throw InvalidParameterException("${method.declaringClass.name} : 2st parameter type is not Packet but ${parameterTypes[1]}")
            if (parameterTypes[2] != ApiSender::class.java) throw InvalidParameterException("${method.declaringClass.name} : 3nd parameter type is not ApiSender but ${parameterTypes[2]}")
            val apiHandler = method.getAnnotation(ApiHandler::class.java) as ApiHandler

            if (methodMap.containsKey(apiHandler.msgName)) throw ApiException.DuplicateApiHandler(apiHandler.msgName)
            methodMap[apiHandler.msgName] = ApiMethod(apiHandler.msgName, method.declaringClass.name, method)
        }
    }
    private fun extractBackendMethod(methods: Set<Method>,methodMap:MutableMap<String,ApiMethod>){
        methods.forEach { method ->
            if (method.parameterCount != 3) throw InvalidParameterException("${method.declaringClass.name} : invalid ApiBackendHandler method parameter count ${method.parameterCount}, ApiBackendHandler method has 3 parameters")
            val parameterTypes = method.parameterTypes
            if (parameterTypes[0] != String::class.java) throw InvalidParameterException("${method.declaringClass.name} : 1st parameter type is not String but ${parameterTypes[0]}")
            if (parameterTypes[1] != Packet::class.java) throw InvalidParameterException("${method.declaringClass.name} : 2st parameter type is not Packet but ${parameterTypes[1]}")
            if (parameterTypes[2] != ApiBackendSender::class.java) throw InvalidParameterException("${method.declaringClass.name} : 3nd parameter type is not ApiBackendSender but ${parameterTypes[2]}")
            val apiHandler = method.getAnnotation(ApiBackendHandler::class.java) as ApiBackendHandler

            if (methodMap.containsKey(apiHandler.msgName)) throw ApiException.DuplicateApiHandler(apiHandler.msgName)
            methodMap[apiHandler.msgName] = ApiMethod(apiHandler.msgName, method.declaringClass.name, method)
        }
    }

    private fun extractInstance(reflections: Reflections) {
        val classes = reflections.getTypesAnnotatedWith(Api::class.java)
        classes.forEach { clazz ->
            val name = clazz.name
            if (isSpringBean(clazz)) {
                instances[name] = ApiInstance(applicationContext.getBean(clazz))
            } else {
                instances[name] = ApiInstance(clazz.getDeclaredConstructor().newInstance())
            }
        }
    }

    companion object {
        fun isSpringBean(clazz: Class<*>): Boolean {
            clazz.annotations.forEach {
                when(it){
                    is Component, is Service, is Controller, is Repository -> return true
                }
            }
            return false
        }
    }

}