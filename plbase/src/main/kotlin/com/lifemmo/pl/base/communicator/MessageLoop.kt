package com.lifemmo.pl.base.communicator;

import org.apache.logging.log4j.kotlin.logger
import java.lang.Thread.sleep

class MessageLoop(private val server: CommunicateServer)  {
    private val logger = logger()
    private lateinit var thread:Thread

    private var running = true;

    fun start() {
        val client = server.getClient()
        this.thread = Thread{
            logger.info("start system loop")
            while(running){
                client.communicate()
                server.communicate()
                sleep(10)
            }
        }.apply { this.start() }
    }

    fun stop(){
        running = false
    }

    fun awaitTermination(){
        this.thread.join()
    }

}