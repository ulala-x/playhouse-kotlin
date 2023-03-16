package org.ulalax.playhouse.communicator;

import org.ulalax.playhouse.LOG


class MessageLoop(private val server: CommunicateServer,
                  private val client: ClientCommunicator)  {

    private var serverThread:Thread = Thread({
        LOG.info("start Server Communicator",this)
        server.communicate()
    },"server:Communicator")

    private var clientThread:Thread = Thread({
        LOG.info("start client Communicator",this)
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