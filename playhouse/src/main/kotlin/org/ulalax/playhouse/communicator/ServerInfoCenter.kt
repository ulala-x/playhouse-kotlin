package org.ulalax.playhouse.communicator

interface ServerInfoCenter {

    fun update(serverList: List<ServerInfoImpl>): List<ServerInfoImpl>
    fun findServer(endpoint: String): ServerInfoImpl
    fun findRoundRobinServer(serviceId:String): ServerInfoImpl
    fun getServerList(): List<ServerInfoImpl>
}