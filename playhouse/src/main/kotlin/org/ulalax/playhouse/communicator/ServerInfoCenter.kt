package org.ulalax.playhouse.communicator

interface IServerInfoCenter {
    fun update(serverList: List<ServerInfo>): List<ServerInfo>
    fun findServer(endpoint: String): ServerInfo
    fun findRoundRobinServer(serviceId:String): ServerInfo
    fun getServerList(): List<ServerInfo>
}