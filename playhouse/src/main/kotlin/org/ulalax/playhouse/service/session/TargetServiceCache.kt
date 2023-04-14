package org.ulalax.playhouse.service.session

import org.ulalax.playhouse.communicator.ServerInfoCenter
import org.ulalax.playhouse.communicator.ServiceType

class TargetServiceCache(private val serverInfoCenter: ServerInfoCenter) {

    private val targetedService = HashMap<Short, ServiceType>()

//    fun findServer(serviceId: Short): XServerInfo {
//        var findServer =  targetedService[serviceId]
//        if(findServer != null && findServer.isValid()){
//            return findServer
//        }else{
//            findServer =  serverInfoCenter.findRoundRobinServer(serviceId)
//            targetedService[serviceId] = findServer
//        }
//        return findServer
//    }
//
//    fun getTargetedServers(): List<XServerInfo> {
//        val lists = ArrayList<XServerInfo>()
//        lists.addAll(targetedService.values.toList())
//        return lists
//    }

    fun findTypeBy(serviceId: Short) : ServiceType{
        var type = targetedService[serviceId]
        if(type == null){
            type = serverInfoCenter.findServerType(serviceId)
            targetedService[serviceId] = type
        }
        return  type
    }

}