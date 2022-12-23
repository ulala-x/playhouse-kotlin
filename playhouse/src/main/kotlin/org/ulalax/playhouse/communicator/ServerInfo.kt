package org.ulalax.playhouse.communicator

enum class ServerState {
    RUNNING,PAUSE,DISABLE
}
interface ServerInfo {



    fun  bindEndpoint():String
    fun serviceType(): ServiceType
    fun serviceId():String
    fun state(): ServerState
    //fun weightingPoint():Int
    fun timeStamp():Long
}