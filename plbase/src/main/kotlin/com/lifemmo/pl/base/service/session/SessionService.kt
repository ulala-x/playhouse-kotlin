package com.lifemmo.pl.base.service.session

import com.lifemmo.pl.base.communicator.*
import com.lifemmo.pl.base.communicator.message.RoutePacket
import com.lifemmo.pl.base.protocol.ClientPacket
import com.lifemmo.pl.base.service.*
import com.lifemmo.pl.base.service.session.network.SessionPacketListener
import com.lifemmo.pl.base.service.session.network.SessionNetwork
import io.netty.channel.Channel
import org.apache.logging.log4j.kotlin.logger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference


class SessionService(private val serviceId:String,
                     private val sessionOption: SessionOption,
                     private val serverInfoCenter: ServerInfoCenterImpl,
                     private val communicateClient: CommunicateClient,
                     private val requestCache: RequestCache,
                     private val sessionPort:Int,
                     private val showQps:Boolean,
                     ) : Service, SessionPacketListener {


    private val log = logger()
    private val clients = ConcurrentHashMap<Int,SessionClient>()
    private var state = AtomicReference(ServerInfo.ServerState.DISABLE)
    private val sessionNetwork = SessionNetwork(sessionOption,this)
    private val performanceTester = PerformanceTester(showQps,"client")
    private val clientQueue = ConcurrentLinkedQueue<Pair<Int,ClientPacket>>()
    private val serverQueue = ConcurrentLinkedQueue<RoutePacket>()
    private lateinit var clientMessageLoopThread:Thread
    private lateinit var serverMessageLoopThread:Thread
    override fun onStart() {

        sessionNetwork.bind(sessionPort)
        state.set(ServerInfo.ServerState.RUNNING)
        performanceTester.start()

        clientMessageLoopThread = Thread{ clientMessageLoop() }
        clientMessageLoopThread.start()

        serverMessageLoopThread = Thread{ serverMessageLoop() }
        serverMessageLoopThread.start()

    }

    private fun clientMessageLoop() {

        while(state.get() != ServerInfo.ServerState.DISABLE) {
            var message = clientQueue.poll()
            while(message!=null){
                val sessionId = message.first
                val clientPacket = message.second

                clientPacket.use {
                    log.debug("SessionService:onReceive ${clientPacket.header.msgName} : from client")
                    val sessionClient = clients[sessionId]
                    if (sessionClient == null) {
                        log.error("sessionId is not exist $sessionId,${clientPacket.msgName()}")
                        return@use
                    }
                    sessionClient.onReceive(clientPacket)
                }
                message = clientQueue.poll()
            }
            Thread.sleep(10)
        }

    }

    private fun serverMessageLoop() {

        while(state.get() != ServerInfo.ServerState.DISABLE) {
            var routePacket = serverQueue.poll()
            while(routePacket!=null){

                routePacket.use {
                    val sessionId = routePacket.routeHeader.sid
                    val packetName = routePacket.msgName()
                    val sessionClient = clients[sessionId]
                    if(sessionClient == null) {
                        log.error("sessionId is already disconnected  $sessionId,$packetName")
                        return
                    }
                    sessionClient.onReceive(routePacket)
                }

                routePacket = serverQueue.poll()
            }
            Thread.sleep(10)
        }

    }

    override fun onReceive(routePacket: RoutePacket) {

        serverQueue.add(routePacket)

    }



//    override fun onReceive(routePacket: RoutePacket) {
//
//        routePacket.use {
//            val sessionId = routePacket.routeHeader.sid
//            val packetName = routePacket.msgName()
//            val sessionClient = clients[sessionId]
//            if(sessionClient == null) {
//                log.error("sessionId is already disconnected  $sessionId,$packetName")
//                return
//            }
//            sessionClient.onReceive(routePacket)
//        }
//    }


    override fun onStop() {
        performanceTester.stop()
        state.set(ServerInfo.ServerState.DISABLE)
        sessionNetwork.shutdown()
    }

    override fun weightPoint(): Int {
        return clients.size
    }

    override fun serverState(): ServerInfo.ServerState {
        return state.get()
    }

    override fun serviceType(): ServiceType {
        return ServiceType.SESSION
    }

    override fun serviceId(): String {
        return serviceId
    }

    override fun pause() {
        this.state.set(ServerInfo.ServerState.PAUSE)
    }

    override fun resume() {
        this.state.set(ServerInfo.ServerState.RUNNING)
    }

    override fun onConnect(channel: Channel) {
        val sessionId = getSessionId(channel)
        if(!clients.contains(sessionId)){
            clients[sessionId] = SessionClient(
                serviceId,
                sessionId,
                channel,
                serverInfoCenter,
                communicateClient,
                sessionOption.urls,
                requestCache)
        }else{
            log.error("sessionId is exist $sessionId")
        }
    }

    override fun onReceive(channel: Channel, clientPacket: ClientPacket) {
        val sessionId = getSessionId(channel)
        clientQueue.add(Pair(sessionId,clientPacket))


    }
//    override fun onReceive(channel: Channel, clientPacket: ClientPacket) {
//
//        clientPacket.use {
//            log.debug("SessionService:onReceive ${clientPacket.header.msgName} : from client")
//            this.performanceTester.incCounter()
//
//            val sessionId = getSessionId(channel)
//            val sessionClient = clients[sessionId]
//            if (sessionClient == null) {
//                log.error("sessionId is not exist $sessionId,${clientPacket.msgName()}")
//                return
//            }
//            sessionClient.onReceive(clientPacket)
//        }
//    }
    override fun onDisconnect(channel: Channel) {
        val sessionId = getSessionId(channel)
        val sessionClient = clients[sessionId]
        if(sessionClient == null) {
            log.error("sessionId is not exist $sessionId")
            return
        }

        sessionClient.disconnect()
        clients.remove(sessionId)
    }

}