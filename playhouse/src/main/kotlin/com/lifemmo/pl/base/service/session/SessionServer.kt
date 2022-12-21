package com.lifemmo.pl.base.service.session

import com.lifemmo.pl.base.communicator.*
import com.lifemmo.pl.base.communicator.zmq.ZmqCommunicateServer
import com.lifemmo.pl.base.service.*
import org.apache.logging.log4j.kotlin.logger

class SessionServer constructor(private val commonOption: CommonOption,private  val sessionOption:SessionOption) : Server {

    private val log = logger()
    private lateinit var communicator:Communicator


    override fun start(){
        val communicatorOption = CommunicatorOption.Builder().apply {
            this.port = commonOption.port
            this.serverSystem = commonOption.serverSystem
            this.showQps = commonOption.showQps
        }.build()

        val bindEndpoint = communicatorOption.bindEndpoint
        val serviceId = commonOption.serviceId

        val communicateServer = ZmqCommunicateServer(bindEndpoint)
        val communicateClient = communicateServer.getClient()

        val requestCache = RequestCache(commonOption.requestTimeoutSec)

        val storageClient = LettuceRedisClient(commonOption.redisIp,commonOption.redisPort).apply { this.connect() }
        val serverInfoCenter = ServerInfoCenterImpl()

        val baseSenderImpl = BaseSenderImpl(serviceId, communicateClient,requestCache)
        val systemPanelImpl = SystemPanelImpl(serverInfoCenter,communicateClient)
        ControlContext.baseSender = baseSenderImpl
        ControlContext.systemPanel = systemPanelImpl


        val sessionService = SessionService(
                serviceId,
                sessionOption,
                serverInfoCenter,
                communicateClient,
                requestCache,
                sessionOption.sessionPort,
                commonOption.showQps
        )

        communicator = Communicator(
            communicatorOption,
            requestCache,
            serverInfoCenter,
            sessionService,
            storageClient,
            baseSenderImpl,
            systemPanelImpl,
            communicateServer,
        )
        communicator.start()

    }

    override fun stop(){
        communicator.stop()
    }
    override fun awaitTermination(){
        communicator.awaitTermination()
    }


}