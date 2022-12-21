package org.ulalax.playhouse.base.communicator
import org.apache.logging.log4j.kotlin.logger
import org.ulalax.playhouse.base.Server


class ServerInfo private constructor(val bindEndpoint: String,
                                     val serviceType:ServiceType,
                                     val serviceId:String,
                                     var state:ServerState,
                                     var weightingPoint:Int,
                                     var timeStamp:Long,
                                        ) {

    private val log = logger()

    companion object{
        fun of(bindEndpoint: String, service:Service):ServerInfo{
            return ServerInfo(
                bindEndpoint,
                service.serviceType(),
                service.serviceId(),
                service.serverState(),
                service.weightPoint(),
                System.currentTimeMillis()
            )
        }
        fun of(infoMsg: Server.ServerInfoMsg):ServerInfo{
            return ServerInfo(
                infoMsg.endpoint,
                ServiceType.valueOf(infoMsg.serviceType),
                infoMsg.serviceId,
                ServerState.valueOf(infoMsg.serverState),
                infoMsg.weightingPoint,
                infoMsg.timestamp,
            )
        }

        fun of(
            bindEndpoint: String,
            serviceType:ServiceType,
            serviceId:String,
            state:ServerState,
            weightingPoint:Int,
            timeStamp:Long,
        ):ServerInfo {
            return ServerInfo(bindEndpoint,serviceType,serviceId,state,weightingPoint,timeStamp)
        }
    }

    fun toMsg(): Server.ServerInfoMsg {
        return Server.ServerInfoMsg.newBuilder().setServiceType(this.serviceType.name)
            .setServiceId(this.serviceId)
            .setEndpoint(this.bindEndpoint)
            .setServerState(this.state.name)
            .setTimestamp(this.timeStamp)
            .setWeightingPoint(this.weightingPoint).build()
    }

    enum class ServerState {
        RUNNING,PAUSE,DISABLE
    }


    fun timeOver():Boolean{

        return  System.currentTimeMillis() - this.timeStamp > 60000
    }

    fun update(serverInfo: ServerInfo):Boolean{
        var stateChanged = false;

        if(this.state!=serverInfo.state){
            stateChanged = true;
        }

        this.state = serverInfo.state

        this.timeStamp = serverInfo.timeStamp

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


}
