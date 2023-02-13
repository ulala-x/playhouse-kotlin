package org.ulalax.playhouse.service.session

import io.netty.channel.Channel
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.protocol.ClientPacket
import org.apache.logging.log4j.kotlin.logger
import org.ulalax.playhouse.communicator.*
import org.ulalax.playhouse.service.RequestCache
import org.ulalax.playhouse.service.session.network.netty.SessionNetwork
import org.ulalax.playhouse.service.session.network.netty.SessionPacketListener
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
    private val clients = ConcurrentHashMap<Int, SessionClient>()
    private var state = AtomicReference(ServerState.DISABLE)
    private val sessionNetwork = SessionNetwork(sessionOption,this);
    private val performanceTester = PerformanceTester(showQps,"client")
    private val clientQueue = ConcurrentLinkedQueue<Pair<Int, ClientPacket>>()
    private val serverQueue = ConcurrentLinkedQueue<RoutePacket>()
    private lateinit var clientMessageLoopThread:Thread
    private lateinit var serverMessageLoopThread:Thread
    override fun onStart() {

        state.set(ServerState.RUNNING)
        performanceTester.start()

        sessionNetwork.bind(sessionPort)

        clientMessageLoopThread = Thread({ clientMessageLoop() },"session:client-message-loop")
        clientMessageLoopThread.start()

        serverMessageLoopThread = Thread({ serverMessageLoop() },"session:server-message-loop")
        serverMessageLoopThread.start()

    }

    private fun clientMessageLoop() {

        while(state.get() != ServerState.DISABLE) {
            var message = clientQueue.poll()
            while(message!=null){
                val sessionId = message.first
                val clientPacket = message.second

                clientPacket.use {
                    log.debug("SessionService:onReceive ${clientPacket.header.msgName} : from client")
                    val sessionClient = clients[sessionId]
                    if (sessionClient == null) {
                        log.error("sessionId is not exist $sessionId,${clientPacket.msgName()}")
                    }else{
                        sessionClient.onReceive(clientPacket)
                    }
                }
                message = clientQueue.poll()
            }
            Thread.sleep(10)
        }

    }

    private fun serverMessageLoop() {

        while(state.get() != ServerState.DISABLE) {
            var routePacket = serverQueue.poll()
            while(routePacket!=null){

                routePacket.use {
                    val sessionId = routePacket.routeHeader.sid
                    val packetName = routePacket.msgName()
                    val sessionClient = clients[sessionId]
                    if(sessionClient == null) {
                        log.error("sessionId is already disconnected  $sessionId,$packetName")
                    }else{
                        sessionClient.onReceive(routePacket)
                    }
                }

                routePacket = serverQueue.poll()
            }
            Thread.sleep(10)
        }

    }

    override fun onReceive(routePacket: RoutePacket) {

        serverQueue.add(routePacket)

    }

    override fun onStop() {
        performanceTester.stop()
        state.set(ServerState.DISABLE)
        sessionNetwork.shutdown()
    }

    override fun weightPoint(): Int {
        return clients.size
    }

    override fun serverState(): ServerState {
        return state.get()
    }

    override fun serviceType(): ServiceType {
        return ServiceType.SESSION
    }

    override fun serviceId(): String {
        return serviceId
    }

    override fun pause() {
        this.state.set(ServerState.PAUSE)
    }

    override fun resume() {
        this.state.set(ServerState.RUNNING)
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
        val sid = getSessionId(channel)
        clientQueue.add(Pair(sid,clientPacket))
    }

    override fun onDisconnect(channel: Channel) {
        val sid = getSessionId(channel)
        val sessionClient = clients[sid]
        if(sessionClient == null) {
            log.error("sessionId is not exist $sid")
            return
        }

        sessionClient.disconnect()
        clients.remove(sid)
    }

}