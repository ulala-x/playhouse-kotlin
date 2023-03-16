package org.ulalax.playhouse.communicator

import org.ulalax.playhouse.LOG
import org.ulalax.playhouse.communicator.socket.PlaySocket

class XServerCommunicator(
        private val playSocket:PlaySocket
) : CommunicateServer {

    private lateinit var listener: CommunicateListener
    private var running = true;

   override fun bind(listener: CommunicateListener) {
        this.listener = listener
        playSocket.bind()
    }
    override fun communicate() {
        while(running){
            var packet = playSocket.receive()
            while(packet !=null){
                try {
                    listener.onReceive(packet)
                }catch (e:Exception){
                    LOG.error("${playSocket.id} Error during communication",this::class.simpleName,e)
                }

                packet = playSocket.receive()
            }
            Thread.sleep(ConstOption.THREAD_SLEEP)
        }

    }
    override fun stop() {
        running = false
    }
}