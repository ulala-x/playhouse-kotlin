package org.ulalax.playhouse.service.api.reflection

import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.ulalax.playhouse.communicator.ClientCommunicator
import org.ulalax.playhouse.communicator.ServerInfoCenter
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.service.*
import org.ulalax.playhouse.service.api.*
import org.mockito.kotlin.mock
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.ulalax.playhouse.communicator.RequestCache
import org.ulalax.playhouse.protocol.Server.*
import org.ulalax.playhouse.protocol.Test.ApiTestMsg1
import org.ulalax.playhouse.service.api.ApiInstance
import org.ulalax.playhouse.service.api.ApiReflection
import org.ulalax.playhouse.service.api.AllApiSender
import org.ulalax.playhouse.service.api.reflection.pojo.back.TestApiBackendService
import org.ulalax.playhouse.service.api.reflection.pojo.front.TestApiService
import org.ulalax.playhouse.service.api.reflection.beans.back.TestApiBackendServiceSpringBeans
import org.ulalax.playhouse.service.api.reflection.beans.front.TestApiServiceSpringBeans

@Configuration
@ComponentScan("org.ulalax.playhouse.service.api.reflection")
open class TestConfigure


object AppContext {
    lateinit var applicationContext:AnnotationConfigApplicationContext
}

class ApiReflectionTest : FunSpec(){

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
            instances.shouldHaveSize(4)
        }

        // init method call test

        test("pojoInitMethodCall"){
            val apiReflection = ApiReflection(TestApiService::class.java.packageName)

            val communicator: ClientCommunicator = mock()
            val serverInfoCenter: ServerInfoCenter = mock()
            val requestCache = RequestCache(5)
            var systemPanelImpl = BaseSystemPanel(serverInfoCenter,communicator)
            var allApiSender = AllApiSender(1, communicator,requestCache)

            apiReflection.callInitMethod(systemPanelImpl,allApiSender)
            resultMessage.shouldBe("init")
        }
        test("pojoBackendInitMethodCall"){
            val apiReflection = ApiReflection(TestApiBackendService::class.java.packageName)

            val communicator: ClientCommunicator = mock()
            val serverInfoCenter: ServerInfoCenter = mock()
            val requestCache = RequestCache(5)
            var systemPanelImpl = BaseSystemPanel(serverInfoCenter,communicator)
            var allApiSender = AllApiSender(1, communicator,requestCache)

            apiReflection.callInitMethod(systemPanelImpl,allApiSender)
            resultMessage.shouldBe("backend init")
        }


        test("apiSpringBeanInitMethodCall"){
            val apiReflection = ApiReflection(TestApiServiceSpringBeans::class.java.packageName)

            val communicator: ClientCommunicator = mock()
            val serverInfoCenter: ServerInfoCenter = mock()
            val requestCache = RequestCache(5)
            var systemPanelImpl = BaseSystemPanel(serverInfoCenter,communicator)
            var allApiSender = AllApiSender(1, communicator,requestCache)

            apiReflection.callInitMethod(systemPanelImpl,allApiSender)
            resultMessage.shouldBe("SpringBeanInit")
        }

        test("backendApiSpringBeanInitMethodCall"){
            val apiReflection = ApiReflection(TestApiBackendServiceSpringBeans::class.java.packageName)

            val communicator: ClientCommunicator = mock()
            val serverInfoCenter: ServerInfoCenter = mock()
            val requestCache = RequestCache(5)
            var allApiSender = AllApiSender(1, communicator,requestCache)

            var systemPanelImpl = BaseSystemPanel(serverInfoCenter,communicator)


            apiReflection.callInitMethod(systemPanelImpl,allApiSender)
            resultMessage.shouldBe("backend SpringBeanInit")
        }

        //registered method call test

        test("apiMethodCall"){
            val apiReflection = ApiReflection(TestApiService::class.java.packageName)

            val routePacketMsg = RoutePacketMsg.newBuilder()
                    .setRouteHeaderMsg(RouteHeaderMsg.newBuilder().setSessionInfo("")
                            .setHeaderMsg(HeaderMsg.newBuilder().setMsgId(1)).setIsBackend(false))
                    .setMessage(ApiTestMsg1.newBuilder().setTestMsg("apiMethodCall").build().toByteString())
                    .build()

            val routePacket = RoutePacket.of(routePacketMsg)
            val apiSenderImpl = AllApiSender(1,object :ClientCommunicator{
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
            val apiReflection = ApiReflection(TestApiBackendService::class.java.packageName)

            val routePacketMsg = RoutePacketMsg.newBuilder()
                    .setRouteHeaderMsg(RouteHeaderMsg.newBuilder().setSessionInfo("")
                            .setHeaderMsg(HeaderMsg.newBuilder().setMsgId(3)).setIsBackend(true))
                    .setMessage(ApiTestMsg1.newBuilder().setTestMsg("apiBackendMethodCall").build().toByteString())
                    .build()

            val routePacket = RoutePacket.of(routePacketMsg)
            val apiSenderImpl = AllApiSender(1,object :ClientCommunicator{
                override fun connect(endpoint: String) {}
                override fun send(endpoint: String, routePacket: RoutePacket) {}
                override fun communicate() {}
                override fun disconnect(endpoint: String) {}
                override fun stop() {}
            }, RequestCache(5))

            apiReflection.callMethod(routePacket.routeHeader,routePacket.toPacket(),routePacket.isBackend(),apiSenderImpl)
            resultMessage.shouldBe("apiBackendMethodCall")
        }

        test("beanApiMethodCall"){
            val apiReflection = ApiReflection(TestApiServiceSpringBeans::class.java.packageName)

            val routePacketMsg = RoutePacketMsg.newBuilder()
                    .setRouteHeaderMsg(RouteHeaderMsg.newBuilder().setSessionInfo("")
                            .setHeaderMsg(HeaderMsg.newBuilder().setMsgId(11)).setIsBackend(false))
                    .setMessage(ApiTestMsg1.newBuilder().setTestMsg("beanApiMethodCall").build().toByteString())
                    .build()

            val routePacket = RoutePacket.of(routePacketMsg)
            val apiSenderImpl = AllApiSender(1,object :ClientCommunicator{
                override fun connect(endpoint: String) {}
                override fun send(endpoint: String, routePacket: RoutePacket) {}
                override fun communicate() {}
                override fun disconnect(endpoint: String) {}
                override fun stop() {}
            }, RequestCache(5))

            apiReflection.callMethod(routePacket.routeHeader,routePacket.toPacket(),routePacket.isBackend(),apiSenderImpl)
            resultMessage.shouldBe("beanApiMethodCall")
        }
        test("beanApiBackendMethodCall"){
            val apiReflection = ApiReflection(TestApiBackendServiceSpringBeans::class.java.packageName)

            val routePacketMsg = RoutePacketMsg.newBuilder()
                    .setRouteHeaderMsg(RouteHeaderMsg.newBuilder().setSessionInfo("")
                            .setHeaderMsg(HeaderMsg.newBuilder().setMsgId(13)).setIsBackend(true))
                    .setMessage(ApiTestMsg1.newBuilder().setTestMsg("beanApiBackendMethodCall").build().toByteString())
                    .build()

            val routePacket = RoutePacket.of(routePacketMsg)
            val apiSenderImpl = AllApiSender(1,object :ClientCommunicator{
                override fun connect(endpoint: String) {}
                override fun send(endpoint: String, routePacket: RoutePacket) {}
                override fun communicate() {}
                override fun disconnect(endpoint: String) {}
                override fun stop() {}
            }, RequestCache(5))

            apiReflection.callMethod(routePacket.routeHeader,routePacket.toPacket(),routePacket.isBackend(),apiSenderImpl)
            resultMessage.shouldBe("beanApiBackendMethodCall")
        }
    }
}