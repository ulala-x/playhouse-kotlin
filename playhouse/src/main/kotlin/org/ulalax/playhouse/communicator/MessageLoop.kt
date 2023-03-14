package org.ulalax.playhouse.communicator;

import org.apache.logging.log4j.kotlin.logger

class MessageLoop(private val server: CommunicateServer, private val client: ClientCommunicator)  {
    private val logger = logger()

    private var serverThread:Thread = Thread({
        logger.info("start Server Communicator")
        server.communicate()
    },"server:Communicator")

    private var clientThread:Thread = Thread({
        logger.info("start client Communicator")
        client.communicate()
    },"client:Communicator")

    fun start() {
        serverThread.start()
        clientThread.start()
    }

    fun stop(){
        server.stop()
        client.stop()
    }

    fun awaitTermination(){
        this.clientThread.join()
        this.serverThread.join()
    }

}