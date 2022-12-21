package org.ulalax.playhouse.base.communicator;

import org.ulalax.playhouse.base.communicator.message.RoutePacket
import org.ulalax.playhouse.base.communicator.zmq.ZmqCommunicateServer
import org.ulalax.playhouse.base.service.*
import org.apache.logging.log4j.kotlin.logger

class CommunicatorOption(
    val bindEndpoint: String,
    val serverSystem:(SystemPanel,BaseSender) -> ServerSystem,
    val showQps:Boolean,
){

    class Builder {
        var port:Int = 0
        lateinit var serverSystem: (SystemPanel,BaseSender) -> ServerSystem
        var showQps:Boolean = false

        fun build(): CommunicatorOption {
            val localIp = IpFinder.findLocalIp()
            val bindEndpoint = String.format("tcp://%s:%d",localIp,port)
            return CommunicatorOption(bindEndpoint,serverSystem,showQps)
        }
    }

}
class Communicator(private val option: CommunicatorOption,
                   private val requestCache: RequestCache,
                   private val serverInfoCenter: ServerInfoCenter,
                   private val service: Service,
                   private var storageClient:StorageClient,
                   private val baseSender:BaseSenderImpl,
                   private val systemPanel:SystemPanelImpl,
                   private val communicateServer: ZmqCommunicateServer
)  : CommunicateListener {

    private val log = logger()
    private lateinit var messageLoop: MessageLoop
    private lateinit var addressResolver:ServerAddressResolver
    private lateinit var baseSystem: BaseSystem
    private val performanceTester = PerformanceTester(option.showQps)



    fun start(){
        val bindEndpoint = option.bindEndpoint
        val system = option.serverSystem
        systemPanel.communicator = this


        //communicateServer = ZmqCommunicateServer(bindEndpoint,this).apply { this.bind() }

        messageLoop = MessageLoop(communicateServer).apply { this.start() }
        addressResolver = ServerAddressResolver(
                                bindEndpoint,
                                serverInfoCenter,
                                communicateServer.getClient(),
                                service,
                                storageClient
                            ).apply { this.start() }


        baseSystem = BaseSystem(system.invoke(systemPanel,baseSender),baseSender).apply { start() }

        communicateServer.bind(this)

        service.onStart()
        performanceTester.start()


        log.info("=========plbase server start==============")
        log.info("Ready for bind:$bindEndpoint")

    }

    private fun updateDisable(){
        storageClient.updateServerInfo(ServerInfo.of(option.bindEndpoint,service).apply {
            state = ServerInfo.ServerState.DISABLE
        })
    }

    fun stop(){
        performanceTester.stop()
        updateDisable()
        baseSystem.stop()
        addressResolver.stop()
        messageLoop.stop()

        log.info("=========server stop==============")
    }
    fun awaitTermination() {
        messageLoop.awaitTermination()
    }

    private fun isPacketToClient(routePacket: RoutePacket) = routePacket.routeHeader.sid > 0
    override fun onReceive(routePacket: RoutePacket) {

        log.debug("onReceive : ${routePacket.msgName()}, from:${routePacket.routeHeader.from}")

        performanceTester.incCounter()

        if ( !isPacketToClient(routePacket) &&  routePacket.isReply()) {
            requestCache.onReply(routePacket)
            return
        }

        if(routePacket.isSystem()){
            this.baseSystem.onReceive(routePacket)
        }else{
            this.service.onReceive(routePacket)
        }
    }

    fun pause() {
        this.service.pause()
        this.baseSystem.pause()
    }

    fun resume() {
        this.service.resume()
        this.baseSystem.resume()
    }

    fun serverState(): ServerInfo.ServerState {
        return this.service.serverState()
    }
}
