package org.ulalax.playhouse.communicator
import org.ulalax.playhouse.communicator.message.RoutePacket
import LOG
import org.ulalax.playhouse.communicator.socket.PlaySocket
import java.util.concurrent.CopyOnWriteArraySet

class XClientCommunicator(private val playSocket: PlaySocket) : ClientCommunicator {

    private val connected:MutableSet<String> = CopyOnWriteArraySet()
    private val disconnected:MutableSet<String> = CopyOnWriteArraySet()
    private val jobBucket = JobBucket()
    private var running = true

    override fun connect(endpoint: String) {
        if(connected.contains(endpoint)) return

        jobBucket.add{
            try {
                playSocket.connect(endpoint)
                connected.add(endpoint)
                disconnected.remove(endpoint)
                LOG.info("connected with $endpoint",this)
            }catch (e:Exception){
                LOG.error( "connect error - endpoint:$endpoint ,error: ${e.message}",this)
            }
        }
    }
    override fun disconnect(endpoint: String) {
        if(disconnected.contains(endpoint)) return

        jobBucket.add{
            try{
                playSocket.disconnect(endpoint)
                LOG.info("disconnected with $endpoint",this)
            }catch (e:Exception){
                LOG.error( "disconnect error - endpoint:$endpoint ,error: ${e.message}",this)
            }
        }
        disconnected.add(endpoint)
        connected.remove(endpoint)
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
                LOG.error("${playSocket.id} socket send error : $endpoint,${routePacket.msgId}",this,e)
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
                    LOG.error("${playSocket.id} Error during communication",this,e)
                }
                action = jobBucket.get()
            }
            Thread.sleep(ConstOption.THREAD_SLEEP)
        }
    }
}