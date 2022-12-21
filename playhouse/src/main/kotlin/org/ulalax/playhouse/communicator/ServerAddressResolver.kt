package org.ulalax.playhouse.communicator

import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.logging.log4j.kotlin.logger
import java.util.*
import kotlin.concurrent.timer

class ServerAddressResolver (
    private val bindEndpoint:String,
    private val serverInfoCenter: ServerInfoCenter,
    private val communicateClient: CommunicateClient,
    private val service: Service,
    private val storageClient: StorageClient
){
    private val log = logger()
    private lateinit var timer: Timer

    fun start(){
        log.info("Server address resolver start")
        timer = timer(period = 1000, initialDelay = 3000) {

            try{

                storageClient.updateServerInfo(
                    ServerInfo.of(
                        bindEndpoint,
                        service.serviceType(),
                        service.serviceId(),
                        service.serverState(),
                        service.weightPoint(),
                        System.currentTimeMillis()
                    )
                )

                val serverInfoList = storageClient.getServerList(bindEndpoint)
                val updateList = serverInfoCenter.update(serverInfoList)

                updateList.forEach { serverInfo->
                    when(serverInfo.state){
                        ServerInfo.ServerState.RUNNING -> communicateClient.connect(serverInfo.bindEndpoint)
                        ServerInfo.ServerState.DISABLE -> communicateClient.disconnect(serverInfo.bindEndpoint)
                        else -> {}
                    }
                }
            }catch (e:Exception){
                log.error(ExceptionUtils.getStackTrace(e))
            }
        }

    }

    fun stop(){
        timer.cancel()
    }
}