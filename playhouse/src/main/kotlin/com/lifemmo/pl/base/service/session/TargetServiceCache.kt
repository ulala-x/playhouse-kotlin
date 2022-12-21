package com.lifemmo.pl.base.service.session

import com.lifemmo.pl.base.communicator.ServerInfo
import com.lifemmo.pl.base.communicator.ServerInfoCenter
import com.lifemmo.pl.base.communicator.ServerInfoCenterImpl
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