package org.ulalax.playhouse.communicator.zmq

import org.apache.logging.log4j.kotlin.logger
import org.ulalax.playhouse.communicator.CommunicateClient
import org.ulalax.playhouse.communicator.CommunicateListener
import org.ulalax.playhouse.communicator.CommunicateServer

class ZmqCommunicateServer(
    bindEndpoint:String
) : CommunicateServer {

    private val log = logger()
    private val zmqSocket: ZmqJSocket = ZmqJSocket(bindEndpoint)
    private val communicateClient = ZmqCommunicateClient(zmqSocket)
    lateinit var listener: CommunicateListener

    override fun bind(listener: CommunicateListener) {
        this.listener = listener
        log.info("${zmqSocket.bindEndpoint} server bind")
        zmqSocket.bind()
    }

    override fun communicate() {
        var packet = zmqSocket.receive()
        while(packet !=null){
            listener.onReceive(packet)
            packet = zmqSocket.receive()
        }
    }

    override fun getClient(): CommunicateClient {
        return communicateClient
    }
}