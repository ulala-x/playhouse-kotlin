package org.ulalax.playhouse.communicator
import org.apache.logging.log4j.kotlin.logger
import org.ulalax.playhouse.communicator.ServerInfo.*
import org.ulalax.playhouse.protocol.Server


class ServerInfoImpl private constructor(val bindEndpoint: String,
                                         val serviceType: ServiceType,
                                         val serviceId:String,
                                         var state: ServerState,
                                         var weightingPoint:Int,
                                         var lastUpdate:Long,
                                        ) : ServerInfo {

    private val log = logger()

    companion object{
        fun of(bindEndpoint: String, service: Service): ServerInfoImpl {
            return ServerInfoImpl(
                bindEndpoint,
                service.serviceType(),
                service.serviceId(),
                service.serverState(),
                service.weightPoint(),
                System.currentTimeMillis()
            )
        }
        fun of(infoMsg: Server.ServerInfoMsg): ServerInfoImpl {
            return ServerInfoImpl(
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
            serviceType: ServiceType,
            serviceId:String,
            state: ServerState,
            weightingPoint:Int,
            timeStamp:Long,
        ): ServerInfoImpl {
            return ServerInfoImpl(bindEndpoint,serviceType,serviceId,state,weightingPoint,timeStamp)
        }
    }

    fun toMsg(): Server.ServerInfoMsg {
        return Server.ServerInfoMsg.newBuilder().setServiceType(this.serviceType.name)
            .setServiceId(this.serviceId)
            .setEndpoint(this.bindEndpoint)
            .setServerState(this.state.name)
            .setTimestamp(this.lastUpdate)
            .setWeightingPoint(this.weightingPoint).build()
    }



    fun timeOver():Boolean{

        return  System.currentTimeMillis() - this.lastUpdate > 60000
    }

    fun update(serverInfo: ServerInfoImpl):Boolean{
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

    override fun serviceId(): String {
        return this.serviceId
    }

    override fun state(): ServerState {
        return this.state
    }

    override fun timeStamp(): Long {
        TODO("Not yet implemented")
    }


}
