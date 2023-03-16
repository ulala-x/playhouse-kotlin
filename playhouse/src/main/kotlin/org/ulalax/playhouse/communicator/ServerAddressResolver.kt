package org.ulalax.playhouse.communicator

import org.apache.commons.lang3.exception.ExceptionUtils
import org.ulalax.playhouse.LOG
import java.util.*
import kotlin.concurrent.timer

class ServerAddressResolver (
        private val bindEndpoint:String,
        private val serverInfoCenter: ServerInfoCenter,
        private val clientCommunicator: ClientCommunicator,
        private val service: Service,
        private val storageClient: StorageClient
){
    private lateinit var timer: Timer
    fun start(){
        LOG.info("Server address resolver start",this::class.simpleName)

        timer = timer(name = "ServerAddressResolverTimer", period = ConstOption.ADDRESS_RESOLVER_PERIOD,
                initialDelay = ConstOption.ADDRESS_RESOLVER_INITIAL_DELAY) {

            try{
                storageClient.updateServerInfo(
                    XServerInfo.of(
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
                        ServerState.RUNNING -> clientCommunicator.connect(serverInfo.bindEndpoint)
                        ServerState.DISABLE -> clientCommunicator.disconnect(serverInfo.bindEndpoint)
                        else -> {}
                    }
                }
            }catch (e:Exception){
                LOG.error(ExceptionUtils.getStackTrace(e),this::class.simpleName,e)
            }
        }
    }
    fun stop(){
        timer.cancel()
    }
}