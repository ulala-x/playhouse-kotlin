package org.ulalax.playhouse.communicator

enum class ServerState {
    RUNNING,PAUSE,DISABLE
}
interface ServerInfo {
    fun  bindEndpoint():String
    fun serviceType(): ServiceType
    fun serviceId():Short
    fun state(): ServerState
    fun timeStamp():Long
}