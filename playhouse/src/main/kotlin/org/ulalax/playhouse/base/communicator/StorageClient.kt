package org.ulalax.playhouse.base.communicator



interface StorageClient {
    fun updateServerInfo(serverInfo: ServerInfo)
    fun getServerList(endpoint:String):List<ServerInfo>
}

