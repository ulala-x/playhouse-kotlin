package org.ulalax.playhouse.service.api

import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.ulalax.playhouse.communicator.ClientCommunicator
import org.ulalax.playhouse.communicator.ServerInfoCenter
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.service.*
import org.ulalax.playhouse.service.api.*
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.ulalax.playhouse.communicator.RequestCache
import org.ulalax.playhouse.protocol.Server.*
import org.ulalax.playhouse.protocol.Test.*
import org.ulalax.playhouse.service.api.pojo.TestApiService
import org.ulalax.playhouse.service.api.beans.TestApiServiceSpringBeans

@Configuration
@ComponentScan("org.ulalax.playhouse.service.api")
open class TestConfigure


object AppContext {
    lateinit var applicationContext:AnnotationConfigApplicationContext
}

class ApiReflectionTest : FunSpec(){

    private val nodeId = 1
    private val accountId:Long = 1
    private val sessionEndpoint = "127.0.0.1:5555"
    private val sid = 2

    override suspend fun beforeSpec(spec: Spec) {
        // 테스트 실행 전에 수행할 코드 작성
        AppContext.applicationContext = AnnotationConfigApplicationContext(TestConfigure::class.java)
    }

    companion object{
        var resultMessage:String =""
    }

    init {
        beforeTest{
        }
        afterTest{
        }

        test("makeClassInstanceMap") {
            val apiReflection = ApiReflection(ApiReflectionTest::class.java.packageName)
            val instances: Map<String, ApiInstance> = apiReflection.instances
            instances.shouldHaveSize(2)
        }

        // init method call test

        test("pojoInitMethodCall"){
            val apiReflection = ApiReflection(TestApiService::class.java.packageName)

            val communicator: ClientCommunicator = mockk(relaxed = true)
            val serverInfoCenter: ServerInfoCenter = mockk(relaxed = true)
            val requestCache = RequestCache(5)
            var systemPanel = XSystemPanel(serverInfoCenter,communicator,nodeId)
            var allApiSender = AllApiSender(1, accountId,sessionEndpoint,sid,communicator,requestCache)

            apiReflection.callInitMethod(systemPanel,allApiSender)
            resultMessage.shouldBe("init")
        }


        test("apiSpringBeanInitMethodCall"){
            val apiReflection = ApiReflection(TestApiServiceSpringBeans::class.java.packageName)

            val communicator: ClientCommunicator = mockk(relaxed = true)
            val serverInfoCenter: ServerInfoCenter = mockk(relaxed = true)
            val requestCache = RequestCache(5)
            var systemPanelImpl = XSystemPanel(serverInfoCenter,communicator,nodeId)
            var allApiSender = AllApiSender(1,accountId,sessionEndpoint,sid, communicator,requestCache)

            apiReflection.callInitMethod(systemPanelImpl,allApiSender)
            resultMessage.shouldBe("SpringBeanInit")
        }


        //registered method call test

        test("apiMethodCall"){
            val apiReflection = ApiReflection(TestApiService::class.java.packageName)

            val routePacketMsg = RoutePacketMsg.newBuilder()
                    .setRouteHeaderMsg(RouteHeaderMsg.newBuilder()
                            .setHeaderMsg(HeaderMsg.newBuilder().setMsgId(ApiTestMsg.getDescriptor().index)).setIsBackend(false))
                    .setMessage(ApiTestMsg.newBuilder().setTestMsg("apiMethodCall").build().toByteString())
                    .build()

            val routePacket = RoutePacket.of(routePacketMsg)
            val apiSenderImpl = AllApiSender(1,accountId,sessionEndpoint,sid,object :ClientCommunicator{
                override fun connect(endpoint: String) {}
                override fun send(endpoint: String, routePacket: RoutePacket) {}
                override fun communicate() {}
                override fun disconnect(endpoint: String) {}
                override fun stop() {}
            }, RequestCache(5))

            apiReflection.callMethod(routePacket.routeHeader,routePacket.toPacket(),routePacket.isBackend(),apiSenderImpl)
            resultMessage.shouldBe("apiMethodCall")
        }


        test("apiBackendMethodCall"){
            val apiReflection = ApiReflection(TestApiService::class.java.packageName)

            val routePacketMsg = RoutePacketMsg.newBuilder()
                    .setRouteHeaderMsg(RouteHeaderMsg.newBuilder()
                            .setHeaderMsg(HeaderMsg.newBuilder().setMsgId(ApiBackendTestMsg.getDescriptor().index))
                            .setIsBackend(true))
                    .setMessage(ApiBackendTestMsg.newBuilder().setTestMsg("apiBackendMethodCall").build().toByteString())
                    .build()

            val routePacket = RoutePacket.of(routePacketMsg)
            val apiSender = AllApiSender(1,accountId,sessionEndpoint,sid,object :ClientCommunicator{
                override fun connect(endpoint: String) {}
                override fun send(endpoint: String, routePacket: RoutePacket) {}
                override fun communicate() {}
                override fun disconnect(endpoint: String) {}
                override fun stop() {}
            }, RequestCache(5))

            apiReflection.callMethod(routePacket.routeHeader,routePacket.toPacket(),routePacket.isBackend(),apiSender)
            resultMessage.shouldBe("apiBackendMethodCall")
        }

        test("beanApiMethodCall"){
            val apiReflection = ApiReflection(TestApiServiceSpringBeans::class.java.packageName)

            val routePacketMsg = RoutePacketMsg.newBuilder()
                    .setRouteHeaderMsg(RouteHeaderMsg.newBuilder()
                            .setHeaderMsg(HeaderMsg.newBuilder().setMsgId(ApiTestBeanMsg.getDescriptor().index)).setIsBackend(false))
                    .setMessage(ApiTestBeanMsg.newBuilder().setTestMsg("beanApiMethodCall").build().toByteString())
                    .build()

            val routePacket = RoutePacket.of(routePacketMsg)
            val apiSender = AllApiSender(1,accountId,sessionEndpoint,sid,object :ClientCommunicator{
                override fun connect(endpoint: String) {}
                override fun send(endpoint: String, routePacket: RoutePacket) {}
                override fun communicate() {}
                override fun disconnect(endpoint: String) {}
                override fun stop() {}
            }, RequestCache(5))

            apiReflection.callMethod(routePacket.routeHeader,routePacket.toPacket(),routePacket.isBackend(),apiSender)
            resultMessage.shouldBe("beanApiMethodCall")
        }
        test("beanApiBackendMethodCall"){
            val apiReflection = ApiReflection(TestApiServiceSpringBeans::class.java.packageName)

            val routePacketMsg = RoutePacketMsg.newBuilder()
                    .setRouteHeaderMsg(RouteHeaderMsg.newBuilder()
                            .setHeaderMsg(HeaderMsg.newBuilder().setMsgId(ApiBackendTestBeanMsg.getDescriptor().index)).setIsBackend(true))
                    .setMessage(ApiBackendTestBeanMsg.newBuilder().setTestMsg("beanApiBackendMethodCall").build().toByteString())
                    .build()

            val routePacket = RoutePacket.of(routePacketMsg)
            val apiSender = AllApiSender(1,accountId,sessionEndpoint,sid,object :ClientCommunicator{
                override fun connect(endpoint: String) {}
                override fun send(endpoint: String, routePacket: RoutePacket) {}
                override fun communicate() {}
                override fun disconnect(endpoint: String) {}
                override fun stop() {}
            }, RequestCache(5))

            apiReflection.callMethod(routePacket.routeHeader,routePacket.toPacket(),routePacket.isBackend(),apiSender)
            resultMessage.shouldBe("beanApiBackendMethodCall")
        }


    }
}