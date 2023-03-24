package org.ulalax.playhouse.service.session

import io.netty.channel.Channel
import org.ulalax.playhouse.LOG
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.communicator.*
import org.ulalax.playhouse.communicator.message.ClientPacket
import org.ulalax.playhouse.service.session.network.netty.SessionNetwork
import org.ulalax.playhouse.service.session.network.netty.SessionListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference


class SessionService(
    override val serviceId:String,
    private val sessionOption: SessionOption,
    private val serverInfoCenter: XServerInfoCenter,
    private val clientCommunicator: ClientCommunicator,
    private val requestCache: RequestCache,
    private val sessionPort:Int,
    private val showQps:Boolean) : Service, SessionListener {

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
                    LOG.debug("SessionService:onReceive ${clientPacket.header.msgName} : from client",this)
                    val sessionClient = clients[sessionId]
                    if (sessionClient == null) {
                        LOG.error("sessionId is not exist $sessionId,${clientPacket.msgName()}",this)
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
                    val packetName = routePacket.getMsgName()
                    val sessionClient = clients[sessionId]
                    if(sessionClient == null) {
                        LOG.error("sessionId is already disconnected  $sessionId,$packetName",this)
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

    override fun getWeightPoint(): Int {
        return clients.size
    }

    override fun getServerState(): ServerState {
        return state.get()
    }

    override fun getServiceType(): ServiceType {
        return ServiceType.SESSION
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
                clientCommunicator,
                sessionOption.urls,
                requestCache)
        }else{
            LOG.error("sessionId is exist $sessionId",this)
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
            LOG.error("sessionId is not exist $sid",this)
            return
        }

        sessionClient.disconnect()
        clients.remove(sid)
    }

}