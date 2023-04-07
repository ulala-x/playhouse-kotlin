package org.ulalax.playhouse.communicator
import org.ulalax.playhouse.protocol.Server


class XServerInfo private constructor(val bindEndpoint: String,
                                      val serviceType: ServiceType,
                                      val serviceId:Short,
                                      var state: ServerState,
                                      var weightingPoint:Int,
                                      var lastUpdate:Long,
                                        ) : ServerInfo {

    companion object{
        fun of(bindEndpoint: String, service: Service): XServerInfo {
            return XServerInfo(
                bindEndpoint,
                service.getServiceType(),
                service.serviceId,
                service.getServerState(),
                service.getWeightPoint(),
                System.currentTimeMillis()
            )
        }
        fun of(infoMsg: Server.ServerInfoMsg): XServerInfo {
            return XServerInfo(
                infoMsg.endpoint,
                ServiceType.valueOf(infoMsg.serviceType),
                infoMsg.serviceId.toShort(),
                ServerState.valueOf(infoMsg.serverState),
                infoMsg.weightingPoint,
                infoMsg.timestamp,
            )
        }

        fun of(
            bindEndpoint: String,
            serviceType: ServiceType,
            serviceId:Short,
            state: ServerState,
            weightingPoint:Int,
            timeStamp:Long,
        ): XServerInfo {
            return XServerInfo(bindEndpoint,serviceType,serviceId,state,weightingPoint,timeStamp)
        }
    }

    fun toMsg(): Server.ServerInfoMsg {
        return Server.ServerInfoMsg.newBuilder().setServiceType(this.serviceType.name)
            .setServiceId(this.serviceId.toInt())
            .setEndpoint(this.bindEndpoint)
            .setServerState(this.state.name)
            .setTimestamp(this.lastUpdate)
            .setWeightingPoint(this.weightingPoint).build()
    }



    fun timeOver():Boolean{

        return  System.currentTimeMillis() - this.lastUpdate > 60000
    }

    fun update(serverInfo: XServerInfo):Boolean{
        var stateChanged = false;

        if(this.state!=serverInfo.state){
            stateChanged = true;
        }

        this.state = serverInfo.state

        this.lastUpdate = serverInfo.lastUpdate

        this.weightingPoint = serverInfo.weightingPoint

        return stateChanged
    }

    fun isValid(): Boolean {
        return this.state == ServerState.RUNNING
    }

    fun toByteArray(): ByteArray {
        return toMsg().toByteArray()
    }

    fun checkTimeout():Boolean {
        if(timeOver()){
            this.state = ServerState.DISABLE
            return true
        }
        return false
    }

    override fun bindEndpoint(): String {
        return this.bindEndpoint
    }

    override fun serviceType(): ServiceType {
        return this.serviceType
    }

    override fun serviceId(): Short {
        return this.serviceId
    }

    override fun state(): ServerState {
        return this.state
    }

    override fun timeStamp(): Long {
        return System.currentTimeMillis()
    }


}
