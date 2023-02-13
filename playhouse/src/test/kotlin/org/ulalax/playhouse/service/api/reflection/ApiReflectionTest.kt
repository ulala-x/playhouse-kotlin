package org.ulalax.playhouse.service.api.reflection

import org.ulalax.playhouse.communicator.CommunicateClient
import org.ulalax.playhouse.communicator.ServerInfoCenter
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.protocol.Packet
import org.ulalax.playhouse.service.*
import org.ulalax.playhouse.service.api.*
import org.ulalax.playhouse.service.api.annotation.Api
import org.ulalax.playhouse.service.api.annotation.ApiBackendHandler
import org.ulalax.playhouse.service.api.annotation.ApiHandler
import org.ulalax.playhouse.service.api.annotation.Init
import org.apache.logging.log4j.kotlin.logger
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.ulalax.playhouse.protocol.Common.HeaderMsg
import org.ulalax.playhouse.protocol.Server.*
import org.ulalax.playhouse.protocol.Test.ApiTestMsg1
import org.ulalax.playhouse.service.api.ApiBaseSenderImpl
import org.ulalax.playhouse.service.api.ApiInstance
import org.ulalax.playhouse.service.api.ApiReflection
import org.ulalax.playhouse.service.api.ApiSenderImpl


internal class ApiReflectionTest {

    private val log = logger()

    companion object{
        var resultValue = ""
    }


    @BeforeEach
    fun setUp() {
    }

    @AfterEach
    fun tearDown() {
    }

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
        fun init(systemPanel: SystemPanel, apiBaseSender: ApiBaseSender){}

        @ApiHandler("ApiHandlerTestMsg1") fun test1(sessionInfo:String, packet: Packet, apiSender: ApiSender){}
        @ApiHandler("ApiHandlerTestMsg2") fun test2(sessionInfo:String, packet: Packet, apiSender: ApiSender){}

        @ApiBackendHandler("ApiBackendHandlerTestMsg2") fun test3(sessionInfo:String, packet: Packet, apiSender: ApiBackendSender){}
    }

    @Api
    @Component
    open class TestApiSpringBeans {

        @Init
        fun init(systemPanel: SystemPanel, apiBaseSender: ApiBaseSender){
            resultValue = apiBaseSender.serviceId()
        }
        @ApiHandler("ApiTestMsg1") fun test1(sessionInfo:String, packet: Packet, apiSender: ApiSender){
            val message = ApiTestMsg1.parseFrom(packet.data())
            resultValue = message.testMsg

        }
        @ApiHandler("ApiTestMsg2  ") fun test2(sessionInfo:String, packet: Packet, apiSender: ApiSender){}

        @ApiBackendHandler("ApiTestMsg2") fun test3(sessionInfo:String, packet: Packet, apiSender: ApiBackendSender){
            resultValue = "message.ApiTestMsg2"
        }
    }


    @Test
    fun makeClassInstanceMap(){
        val applicationContext = AnnotationConfigApplicationContext(TestConfigure::class.java)
        val apiReflection = ApiReflection(TestApiReflections::class.java.packageName,applicationContext)

        val instances:Map<String, ApiInstance> =apiReflection.instances
        assertThat(instances.size).isEqualTo(2)
        instances.forEach{assertThat(it.value.instance).isInstanceOfAny(TestApiReflections::class.java, TestApiSpringBeans::class.java)}
    }

    @Test
    fun apiInitMethodCall(){
        val applicationContext = AnnotationConfigApplicationContext(TestConfigure::class.java)
        val apiReflection = ApiReflection(TestApiReflections::class.java.packageName,applicationContext)

        val communicateClient: CommunicateClient = mock()
        val serverInfoCenter: ServerInfoCenter = mock()
        val requestCache = RequestCache(5)
        var systemPanelImpl = SystemPanelImpl(serverInfoCenter,communicateClient)
        var apiBaseSenderImpl = ApiBaseSenderImpl("test", communicateClient,requestCache)

        apiReflection.callInitMethod(systemPanelImpl,apiBaseSenderImpl)
        assertThat(resultValue).isEqualTo("test")
    }

    @Test
    fun apiHandlerMethodCall(){
        val applicationContext = AnnotationConfigApplicationContext(TestConfigure::class.java)
        val apiReflection = ApiReflection(TestApiReflections::class.java.packageName,applicationContext)

        val routePacketMsg = RoutePacketMsg.newBuilder()
            .setRouteHeaderMsg(RouteHeaderMsg.newBuilder().setSessionInfo("").setHeaderMsg(HeaderMsg.newBuilder().setMsgName("ApiTestMsg1")))
            .setMessage(ApiTestMsg1.newBuilder().setTestMsg("reflection").build().toByteString())
            .build()


        val routePacket = RoutePacket.of(routePacketMsg)
        val apiSenderImpl = ApiSenderImpl("",object :CommunicateClient{
            override fun connect(endpoint: String) {}
            override fun send(endpoint: String, routePacket: RoutePacket) {}
            override fun communicate() {}
            override fun disconnect(endpoint: String) {}
        }, RequestCache(5))

        apiReflection.callMethod(routePacket.routeHeader,routePacket.toPacket(),routePacket.isBackend(),apiSenderImpl)
        assertThat(resultValue).isEqualTo("reflection")
    }

    @Test
    fun apiBackendMethodCall(){
        val applicationContext = AnnotationConfigApplicationContext(TestConfigure::class.java)
        val apiReflection = ApiReflection(TestApiReflections::class.java.packageName,applicationContext)

        val routePacketMsg = RoutePacketMsg.newBuilder()
            .setRouteHeaderMsg(RouteHeaderMsg.newBuilder().setSessionInfo("")
                .setHeaderMsg(HeaderMsg.newBuilder().setMsgName("ApiTestMsg2")).setIsBackend(true))
            .setMessage(ApiTestMsg1.newBuilder().setTestMsg("reflection").build().toByteString())
            .build()

        val routePacket = RoutePacket.of(routePacketMsg)
        val apiSenderImpl = ApiSenderImpl("",object :CommunicateClient{
            override fun connect(endpoint: String) {}
            override fun send(endpoint: String, routePacket: RoutePacket) {}
            override fun communicate() {}
            override fun disconnect(endpoint: String) {}
        }, RequestCache(5))

        apiReflection.callMethod(routePacket.routeHeader,routePacket.toPacket(),routePacket.isBackend(),apiSenderImpl)
        assertThat(resultValue).isEqualTo("message.ApiTestMsg2")
    }

    @Test
    fun checkSpringBean(){

        @Component open class TestComponent
        @Repository open class TestRepository
        @Service open class TestService
        @Controller open class TestController
        open class TestClass


        assertThat(ApiReflection.isSpringBean(TestComponent::class.java)).isTrue
        assertThat(ApiReflection.isSpringBean(TestRepository::class.java)).isTrue
        assertThat(ApiReflection.isSpringBean(TestService::class.java)).isTrue
        assertThat(ApiReflection.isSpringBean(TestController::class.java)).isTrue
        assertThat(ApiReflection.isSpringBean(TestClass::class.java)).isFalse

    }


}