package org.ulalax.playhouse.service.play

import org.ulalax.playhouse.communicator.XServerCommunicator
import org.ulalax.playhouse.communicator.*
import org.ulalax.playhouse.communicator.socket.PlaySocketFactory
import org.ulalax.playhouse.communicator.socket.SocketConfig
import org.ulalax.playhouse.service.*

class PlayServer constructor(private val commonOption: CommonOption,
                             private val playOption: PlayOption) : Server {


    private lateinit var communicator: Communicator

    override fun start() {

        val communicatorOption = CommunicatorOption.Builder().apply {
            this.port = commonOption.port
            this.serverSystem = commonOption.serverSystem
            this.showQps = commonOption.showQps
        }.build()

        val bindEndpoint = communicatorOption.bindEndpoint
        val serviceId = commonOption.serviceId

        val communicateServer = XServerCommunicator(PlaySocketFactory.createPlaySocket(SocketConfig(), bindEndpoint))
        val communicateClient = XClientCommunicator(PlaySocketFactory.createPlaySocket(SocketConfig(), bindEndpoint))


        val requestCache = RequestCache(commonOption.requestTimeoutSec)

        val storageClient = LettuceRedisClient(commonOption.redisIp,commonOption.redisPort).apply { this.connect() }
        val serverInfoCenter = XServerInfoCenter()

        val XSender = XSender(serviceId, communicateClient,requestCache)

        val nodeId = storageClient.getNodeId(bindEndpoint)
        val systemPanelImpl = XSystemPanel(serverInfoCenter,communicateClient,nodeId)
        ControlContext.baseSender = XSender
        ControlContext.systemPanel = systemPanelImpl

        val playService = PlayProcessor(serviceId, bindEndpoint, playOption, communicateClient, requestCache,serverInfoCenter)

        communicator = Communicator(
            communicatorOption,
            requestCache,
            serverInfoCenter,
            playService,
            storageClient,
            XSender,
            systemPanelImpl,
            communicateServer,
            communicateClient
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