package org.ulalax.playhouse.communicator

enum class ServerState {
    RUNNING,PAUSE,DISABLE
}
interface IServerInfo {
    fun  bindEndpoint():String
    fun serviceType(): ServiceType
    fun serviceId():String
    fun state(): ServerState
    fun timeStamp():Long
}