package org.ulalax.playhouse.communicator


import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.Logger
import org.ulalax.playhouse.communicator.socket.PlaySocket
import java.util.concurrent.CopyOnWriteArraySet

class XClientCommunicator(private val playSocket: PlaySocket, private val log: Logger) : ClientCommunicator {

    private val connected:MutableSet<String> = CopyOnWriteArraySet()
    private val disconnected:MutableSet<String> = CopyOnWriteArraySet()
    private val jobBucket = JobBucket()
    private var running = true

    override fun connect(endpoint: String) {
        if(connected.contains(endpoint)) return

        jobBucket.add{
            playSocket.connect(endpoint)
            connected.add(endpoint)
            disconnected.remove(endpoint)
        }
    }
    override fun disconnect(endpoint: String) {
        if(disconnected.contains(endpoint)) return

        jobBucket.add{
            playSocket.disconnect(endpoint)
            disconnected.add(endpoint)
            connected.remove(endpoint)
        }
    }

    override fun stop() {
        running = false
    }

    override fun send(endpoint: String, routePacket: RoutePacket) {
        jobBucket.add{
            try{
                routePacket.use {
                    playSocket.send(endpoint,routePacket)
                }
            }catch (e:Exception){
                log.error("${playSocket.id} socket send error : $endpoint,${routePacket.msgName()}",this::class.simpleName,e)
            }
        }
    }

    override fun communicate() {
        while(running){
            var action:Action? = jobBucket.get()
            while(action!=null){
                try {
                    action.invoke()
                }catch (e:Exception){
                    log.error("${playSocket.id} Error during communication",this::class.simpleName,e)
                }
                action = jobBucket.get()
            }
            Thread.sleep(ConstOption.THREAD_SLEEP)
        }
    }
}