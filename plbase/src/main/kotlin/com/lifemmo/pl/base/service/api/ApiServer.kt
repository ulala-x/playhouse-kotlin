package com.lifemmo.pl.base.service.api

import com.lifemmo.pl.base.communicator.*
import com.lifemmo.pl.base.communicator.zmq.ZmqCommunicateServer
import com.lifemmo.pl.base.service.*
import org.apache.logging.log4j.kotlin.logger

class ApiServer(private val commonOption: CommonOption,private val apiOption: ApiOption):Server {
    private val log = logger()
    private lateinit var communicator: Communicator

    override fun start() {

        val communicatorOption = CommunicatorOption.Builder().apply {
            this.port = commonOption.port
            this.serverSystem = commonOption.serverSystem
            this.showQps = commonOption.showQps
        }.build()

        val bindEndpoint = communicatorOption.bindEndpoint
        val serviceId = commonOption.serviceId


        val requestCache = RequestCache(commonOption.requestTimeoutSec)
        val storageClient = LettuceRedisClient(commonOption.redisIp,commonOption.redisPort).apply { this.connect() }
        val serverInfoCenter = ServerInfoCenterImpl()

        val communicateServer = ZmqCommunicateServer(bindEndpoint)
        val communicateClient = communicateServer.getClient()

        val apiBaseSenderImpl = ApiBaseSenderImpl(serviceId, communicateClient,requestCache)
        val systemPanelImpl = SystemPanelImpl(serverInfoCenter,communicateClient)
        ControlContext.baseSender = apiBaseSenderImpl
        ControlContext.systemPanel = systemPanelImpl

        val service = ApiService(serviceId, apiOption, requestCache, communicateClient,apiBaseSenderImpl,systemPanelImpl)
        communicator = Communicator(
            communicatorOption,
            requestCache,
            serverInfoCenter,
            service,
            storageClient,
            apiBaseSenderImpl,
            systemPanelImpl,
            communicateServer,
        )


        communicator.start()

    }

    override fun stop() {
        communicator.stop()
    }

    override fun awaitTermination() {
        communicator.awaitTermination()
    }
}