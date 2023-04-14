package org.ulalax.playhouse.communicator

interface ServerInfoCenter {
    fun update(serverList: List<XServerInfo>): List<XServerInfo>
    fun findServer(endpoint: String): XServerInfo
    fun findRoundRobinServer(serviceId:Short): XServerInfo
    fun getServerList(): List<XServerInfo>
    fun findServerByAccountId(serviceId: Short, accountId: Long): XServerInfo
    fun findServerType(serviceId: Short) : ServiceType
}