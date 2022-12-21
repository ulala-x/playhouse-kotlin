package org.ulalax.playhouse.base.service.session

import org.ulalax.playhouse.base.communicator.ServerInfo
import org.ulalax.playhouse.base.communicator.ServerInfoCenter
import org.ulalax.playhouse.base.communicator.ServerInfoCenterImpl
import org.apache.logging.log4j.kotlin.logger

class TargetServiceCache(private val serverInfoCenter: ServerInfoCenter) {

    private val log = logger()

    private val targetedService = HashMap<String,ServerInfo>()

    fun findServer(serviceId: String): ServerInfo {
        var findServer =  targetedService[serviceId]
        if(findServer != null && findServer.isValid()){
            return findServer
        }else{
            findServer =  serverInfoCenter.findRoundRobinServer(serviceId)
            targetedService[serviceId] = findServer
        }
        return findServer
    }

    fun getTargetedServers(): List<ServerInfo> {
        val lists = ArrayList<ServerInfo>()
        lists.addAll(targetedService.values.toList())
        return lists
    }

}