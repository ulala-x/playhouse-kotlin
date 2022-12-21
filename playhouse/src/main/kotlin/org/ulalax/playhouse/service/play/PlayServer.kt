package org.ulalax.playhouse.service.play

import org.ulalax.playhouse.communicator.zmq.ZmqCommunicateServer
import org.apache.logging.log4j.kotlin.logger
import org.ulalax.playhouse.communicator.*
import org.ulalax.playhouse.service.*

class PlayServer constructor(private val commonOption: CommonOption, private val playOption: PlayOption) : Server {

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

        val communicateServer = ZmqCommunicateServer(bindEndpoint)
        val communicateClient = communicateServer.getClient()


        val requestCache = RequestCache(commonOption.requestTimeoutSec)

        val storageClient = LettuceRedisClient(commonOption.redisIp,commonOption.redisPort).apply { this.connect() }
        val serverInfoCenter = ServerInfoCenterImpl()

        val baseSenderImpl = BaseSenderImpl(serviceId, communicateClient,requestCache)
        val systemPanelImpl = SystemPanelImpl(serverInfoCenter,communicateClient)
        ControlContext.baseSender = baseSenderImpl
        ControlContext.systemPanel = systemPanelImpl

        val playService = PlayService(serviceId, bindEndpoint, playOption, communicateClient, requestCache,serverInfoCenter)

        communicator = Communicator(
            communicatorOption,
            requestCache,
            serverInfoCenter,
            playService,
            storageClient,
            baseSenderImpl,
            systemPanelImpl,
            communicateServer
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