package org.ulalax.playhouse.service.session

import org.ulalax.playhouse.communicator.XServerCommunicator
import org.ulalax.playhouse.communicator.*
import org.ulalax.playhouse.communicator.socket.ZmqJPlaySocket
import org.ulalax.playhouse.service.*

class SessionServer constructor(private val commonOption: CommonOption, private  val sessionOption: SessionOption) : Server {

    private lateinit var communicator: Communicator


    override fun start(){
        val communicatorOption = CommunicatorOption.Builder().apply {
            this.port = commonOption.port
            this.serverSystem = commonOption.serverSystem
            this.showQps = commonOption.showQps
        }.build()

        val bindEndpoint = communicatorOption.bindEndpoint
        val serviceId = commonOption.serviceId

        val communicateServer = XServerCommunicator(ZmqJPlaySocket(bindEndpoint))
        val communicateClient = XClientCommunicator(ZmqJPlaySocket(bindEndpoint))

        val requestCache = RequestCache(commonOption.requestTimeoutSec)

        val storageClient = LettuceRedisClient(commonOption.redisIp,commonOption.redisPort).apply { this.connect() }
        val serverInfoCenter = XServerInfoCenter()

        val baseSenderImpl = BaseSender(serviceId, communicateClient,requestCache)
        val systemPanelImpl = BaseSystemPanel(serverInfoCenter,communicateClient)

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
            communicateClient
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