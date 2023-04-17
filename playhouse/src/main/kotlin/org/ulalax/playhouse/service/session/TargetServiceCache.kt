package org.ulalax.playhouse.service.session

import org.ulalax.playhouse.communicator.ServerInfoCenter
import org.ulalax.playhouse.communicator.ServiceType

class TargetServiceCache(private val serverInfoCenter: ServerInfoCenter) {

    private val targetedService = HashMap<Short, ServiceType>()

    fun findTypeBy(serviceId: Short) : ServiceType{
        var type = targetedService[serviceId]
        if(type == null){
            type = serverInfoCenter.findServerType(serviceId)
            targetedService[serviceId] = type
        }
        return  type
    }

}