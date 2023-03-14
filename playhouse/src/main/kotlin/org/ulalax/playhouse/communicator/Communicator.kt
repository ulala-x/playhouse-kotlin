package org.ulalax.playhouse.communicator;

import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.Logger
import org.ulalax.playhouse.service.*

class CommunicatorOption(
        val bindEndpoint: String,
        val serverSystem:(SystemPanel, CommonSender) -> ServerSystem,
        val showQps:Boolean,
){
    class Builder {
        var port:Int = 0
        lateinit var serverSystem: (SystemPanel, CommonSender) -> ServerSystem
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
                   private var storageClient: StorageClient,
                   private val baseSender: BaseSender,
                   private val systemPanel: BaseSystemPanel,
                   private val communicateServer: XServerCommunicator,
                   private val communicateClient: XClientCommunicator,
                   private val log : Logger
)  : CommunicateListener {


    private lateinit var messageLoop: MessageLoop
    private lateinit var addressResolver: ServerAddressResolver
    private lateinit var baseSystem: BaseSystem
    private val performanceTester = PerformanceTester(option.showQps,log)

    fun start(){
        val bindEndpoint = option.bindEndpoint
        val system = option.serverSystem
        systemPanel.communicator = this

        messageLoop = MessageLoop(communicateServer,communicateClient).apply { this.start() }
        addressResolver = ServerAddressResolver(
                                bindEndpoint,
                                serverInfoCenter,
                                communicateClient,
                                service,
                                storageClient,
                                log
                            ).apply { this.start() }


        baseSystem = BaseSystem(system.invoke(systemPanel,baseSender),baseSender).apply { start() }

        communicateServer.bind(this)

        service.onStart()
        performanceTester.start()

        log.info("============== server start ==============",this::class.simpleName)
        log.info("Ready for bind:$bindEndpoint",this::class.simpleName)
    }

    private fun updateDisable(){
        storageClient.updateServerInfo(XServerInfo.of(option.bindEndpoint, service).apply {
            state = ServerState.DISABLE
        })
    }

    fun stop(){
        performanceTester.stop()
        updateDisable()
        baseSystem.stop()
        addressResolver.stop()
        messageLoop.stop()

        log.info("============== server stop ==============",this::class.simpleName)
    }
    fun awaitTermination() {
        messageLoop.awaitTermination()
    }

    private fun isPacketToClient(routePacket: RoutePacket) = routePacket.routeHeader.sid > 0
    override fun onReceive(routePacket: RoutePacket) {

        log.debug("onReceive : ${routePacket.msgName()}, from:${routePacket.routeHeader.from}",this::class.simpleName)

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

    fun serverState(): ServerState {
        return this.service.serverState()
    }
}
