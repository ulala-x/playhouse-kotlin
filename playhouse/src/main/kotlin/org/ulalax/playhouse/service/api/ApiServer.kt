package org.ulalax.playhouse.service.api

import org.ulalax.playhouse.communicator.XServerCommunicator
import org.ulalax.playhouse.communicator.*
import org.ulalax.playhouse.communicator.socket.PlaySocketFactory
import org.ulalax.playhouse.communicator.socket.SocketConfig
import org.ulalax.playhouse.service.ControlContext
import org.ulalax.playhouse.service.Server
import org.ulalax.playhouse.service.XSystemPanel
import org.ulalax.playhouse.service.XSender

class ApiServer(private val commonOption: CommonOption, private val apiOption: ApiOption): Server {

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
        val storageClient = RedisStorageClient(commonOption.redisIp,commonOption.redisPort).apply { this.connect() }

        val serverInfoCenter = XServerInfoCenter()

        val communicateServer = XServerCommunicator(PlaySocketFactory.createPlaySocket(SocketConfig(), bindEndpoint))
        val communicateClient = XClientCommunicator(PlaySocketFactory.createPlaySocket(SocketConfig(), bindEndpoint))

        val sender = XSender(serviceId, communicateClient,requestCache)

        val nodeId = storageClient.getNodeId(bindEndpoint)


        val systemPanel = XSystemPanel(serverInfoCenter,communicateClient,nodeId)
        ControlContext.baseSender = sender
        ControlContext.systemPanel = systemPanel

        val service = ApiProcessor(serviceId, apiOption, requestCache, communicateClient,sender,systemPanel)
        communicator = Communicator(
            communicatorOption,
            requestCache,
            serverInfoCenter,
            service,
            storageClient,
            sender,
            systemPanel,
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