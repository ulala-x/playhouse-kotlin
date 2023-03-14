package org.ulalax.playhouse.service.api.reflection

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.ulalax.playhouse.communicator.ClientCommunicator
import org.ulalax.playhouse.communicator.ServerInfoCenter
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.service.*
import org.ulalax.playhouse.service.api.*
import org.ulalax.playhouse.service.api.annotation.Api
import org.ulalax.playhouse.service.api.annotation.ApiBackendHandler
import org.ulalax.playhouse.service.api.annotation.ApiHandler
import org.ulalax.playhouse.service.api.annotation.Init
import org.mockito.kotlin.mock
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.ulalax.playhouse.ConsoleLogger
import org.ulalax.playhouse.communicator.RequestCache
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.protocol.Common.HeaderMsg
import org.ulalax.playhouse.protocol.Server.*
import org.ulalax.playhouse.protocol.Test.ApiTestMsg1
import org.ulalax.playhouse.service.api.ApiBaseSender
import org.ulalax.playhouse.service.api.ApiInstance
import org.ulalax.playhouse.service.api.ApiReflection
import org.ulalax.playhouse.service.api.BaseApiSender


class ApiReflectionTest : FunSpec(){

    // TODO: 2022-09-06
    // 패키지 url 에서 @api 어노테이션을 가진 class 리스트 목록을 찾는다.
    // class 리스트 목록을 가지고 instance map 을 만든다.
    // instance map 을 만들때 spring bean 이면 bean 인스턴스를 만든다.
    // @apiHandler 어노테이션을 가지고 api handler map 리스트를 만든다.
    // api handler 를 map 에 등록할때 올바른 파라메터를 가지고 있는지 검증한다.
    // 요청 packet을 처리할수 있는 api handler 를 찾아서 정상적으로 호출되는지 확인한다.
    // spring bean 으로 등록된 class 의 method 에서 spring DI 가 동작하는지 확인한다.

    @Configuration @ComponentScan("org.ulalax.playhouse.service.api.reflection") open class TestConfigure

    @Api
    class TestApiReflections {

        @Init
        fun init(@Suppress("UNUSED_PARAMETER")systemPanel: SystemPanel,
                 @Suppress("UNUSED_PARAMETER")apiBaseSender: ApiCommonSender){}

        @ApiHandler("ApiHandlerTestMsg1") fun test1(
                @Suppress("UNUSED_PARAMETER")sessionInfo:String,
                @Suppress("UNUSED_PARAMETER")packet: Packet,
                @Suppress("UNUSED_PARAMETER")apiSender: ApiSender){}

        @ApiHandler("ApiHandlerTestMsg2") fun test2(
                @Suppress("UNUSED_PARAMETER")sessionInfo:String,
                @Suppress("UNUSED_PARAMETER")packet: Packet,
                @Suppress("UNUSED_PARAMETER")apiSender: ApiSender){}

        @ApiBackendHandler("ApiBackendHandlerTestMsg2") fun test3(
                @Suppress("UNUSED_PARAMETER")sessionInfo:String,
                @Suppress("UNUSED_PARAMETER")packet: Packet,
                @Suppress("UNUSED_PARAMETER")apiSender: ApiBackendSender){}
    }

    companion object{
        var resultValue = ""
    }

    @Api
    @Component
    open class TestApiSpringBeans {

        @Init
        fun init(@Suppress("UNUSED_PARAMETER")systemPanel: SystemPanel, apiBaseSender: ApiCommonSender){
            resultValue = apiBaseSender.serviceId()
        }
        @ApiHandler("ApiTestMsg1") fun test1(
                @Suppress("UNUSED_PARAMETER") sessionInfo:String,
                packet: Packet,
                @Suppress("UNUSED_PARAMETER") apiSender: ApiSender){
            val message = ApiTestMsg1.parseFrom(packet.data())
            resultValue = message.testMsg
        }

        @ApiHandler("ApiTestMsg2  ") fun test2(
                @Suppress("UNUSED_PARAMETER") sessionInfo:String,
                @Suppress("UNUSED_PARAMETER") packet: Packet,
                @Suppress("UNUSED_PARAMETER") apiSender: ApiSender){}

        @ApiBackendHandler("ApiTestMsg2") fun test3(
                @Suppress("UNUSED_PARAMETER")sessionInfo:String,
                @Suppress("UNUSED_PARAMETER")packet: Packet,
                @Suppress("UNUSED_PARAMETER")apiSender: ApiBackendSender){

            resultValue = "message.ApiTestMsg2"
        }
    }

    init {
        beforeTest{

        }
        afterTest{

        }

        test("makeClassInstanceMap") {
            val applicationContext = AnnotationConfigApplicationContext(TestConfigure::class.java)
            val apiReflection = ApiReflection(TestApiReflections::class.java.packageName, applicationContext, ConsoleLogger())

            val instances: Map<String, ApiInstance> = apiReflection.instances

            instances.shouldHaveSize(2)
            instances.forEach {
                val instance = it.value.instance
                (instance is TestApiReflections || instance is TestApiSpringBeans).shouldBeTrue()
            }
        }


        test("apiInitMethodCall"){
            val applicationContext = AnnotationConfigApplicationContext(TestConfigure::class.java)
            val apiReflection = ApiReflection(TestApiReflections::class.java.packageName,applicationContext,ConsoleLogger())

            val IClientCommunicator: ClientCommunicator = mock()
            val serverInfoCenter: ServerInfoCenter = mock()
            val requestCache = RequestCache(5,ConsoleLogger())
            var systemPanelImpl = BaseSystemPanel(serverInfoCenter,IClientCommunicator)
            var apiBaseSenderImpl = ApiBaseSender("test", IClientCommunicator,requestCache)

            apiReflection.callInitMethod(systemPanelImpl,apiBaseSenderImpl)
            resultValue.shouldBe("test")
        }


        test("apiBackendMethodCall"){
            val applicationContext = AnnotationConfigApplicationContext(TestConfigure::class.java)
            val apiReflection = ApiReflection(TestApiReflections::class.java.packageName,applicationContext,ConsoleLogger())

            val routePacketMsg = RoutePacketMsg.newBuilder()
                    .setRouteHeaderMsg(RouteHeaderMsg.newBuilder().setSessionInfo("")
                            .setHeaderMsg(HeaderMsg.newBuilder().setMsgName("ApiTestMsg2")).setIsBackend(true))
                    .setMessage(ApiTestMsg1.newBuilder().setTestMsg("reflection").build().toByteString())
                    .build()

            val routePacket = RoutePacket.of(routePacketMsg)
            val apiSenderImpl = BaseApiSender("",object :ClientCommunicator{
                override fun connect(endpoint: String) {}
                override fun send(endpoint: String, routePacket: RoutePacket) {}
                override fun communicate() {}
                override fun disconnect(endpoint: String) {}
                override fun stop() {}
            }, RequestCache(5,ConsoleLogger()))

            apiReflection.callMethod(routePacket.routeHeader,routePacket.toPacket(),routePacket.isBackend(),apiSenderImpl)
            resultValue.shouldBe("message.ApiTestMsg2")
        }

        test("checkSpringBean"){
            @Component open class TestComponent
            @Repository open class TestRepository
            @Service open class TestService
            @Controller open class TestController
            open class TestClass

            ApiReflection.isSpringBean(TestComponent::class.java).shouldBeTrue()
            ApiReflection.isSpringBean(TestRepository::class.java).shouldBeTrue()
            ApiReflection.isSpringBean(TestService::class.java).shouldBeTrue()
            ApiReflection.isSpringBean(TestController::class.java).shouldBeTrue()
            ApiReflection.isSpringBean(TestClass::class.java).shouldBeFalse()
        }
    }
}