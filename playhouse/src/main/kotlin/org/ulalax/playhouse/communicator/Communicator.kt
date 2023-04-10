package org.ulalax.playhouse.communicator;

import LOG
import org.ulalax.playhouse.communicator.message.RoutePacket
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
                   private val processor: Processor,
                   private var storageClient: StorageClient,
                   private val baseSender: BaseSender,
                   private val systemPanel: BaseSystemPanel,
                   private val communicateServer: XServerCommunicator,
                   private val communicateClient: XClientCommunicator,
)  : CommunicateListener {


    private var messageLoop: MessageLoop = MessageLoop(communicateServer,communicateClient)
    private var addressResolver: ServerAddressResolver = ServerAddressResolver(
        option.bindEndpoint,
        serverInfoCenter,
        communicateClient,
        processor,
        storageClient
    )
    private var baseSystem: BaseSystem = BaseSystem(option.serverSystem.invoke(systemPanel,baseSender),baseSender)
    private val performanceTester = PerformanceTester(option.showQps)

    fun start(){
        val bindEndpoint = option.bindEndpoint

        systemPanel.communicator = this

        messageLoop.start()
        addressResolver.start()
        baseSystem.start()

        communicateServer.bind(this)

        processor.onStart()
        performanceTester.start()

        LOG.info("============== server start ==============",this)
        LOG.info("Ready for bind:$bindEndpoint",this)
    }

    private fun updateDisable(){
        storageClient.updateServerInfo(XServerInfo.of(option.bindEndpoint, processor).apply {
            state = ServerState.DISABLE
        })
    }

    fun stop(){
        performanceTester.stop()
        updateDisable()
        baseSystem.stop()
        addressResolver.stop()
        messageLoop.stop()

        LOG.info("============== server stop ==============",this)
    }
    fun awaitTermination() {
        messageLoop.awaitTermination()
    }

    private fun isPacketToClient(routePacket: RoutePacket) = routePacket.routeHeader.sid > 0
    override fun onReceive(routePacket: RoutePacket) {

        LOG.debug("onReceive : ${routePacket.msgId()}, from:${routePacket.routeHeader.from}",this)

        performanceTester.incCounter()

        if ( !isPacketToClient(routePacket) &&  routePacket.isReply()) {
            requestCache.onReply(routePacket)
            return
        }

        if(routePacket.isSystem()){
            this.baseSystem.onReceive(routePacket)
        }else{
            this.processor.onReceive(routePacket)
        }
    }

    fun pause() {
        this.processor.pause()
        this.baseSystem.pause()
    }

    fun resume() {
        this.processor.resume()
        this.baseSystem.resume()
    }

    fun serverState(): ServerState {
        return this.processor.getServerState()
    }
}
