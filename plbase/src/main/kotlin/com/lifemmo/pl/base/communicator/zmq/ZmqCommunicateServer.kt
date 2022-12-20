package com.lifemmo.pl.base.communicator.zmq

import com.lifemmo.pl.base.communicator.*
import org.apache.logging.log4j.kotlin.logger

class ZmqCommunicateServer(
    bindEndpoint:String
) : CommunicateServer {

    private val log = logger()
    private val zmqSocket:ZmqSocket = JZmqSocket(bindEndpoint)
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