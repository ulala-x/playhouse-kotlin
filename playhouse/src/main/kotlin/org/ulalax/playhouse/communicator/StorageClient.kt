package org.ulalax.playhouse.communicator



interface StorageClient {
    fun updateServerInfo(serverInfo: ServerInfo)
    fun getServerList(endpoint:String):List<ServerInfo>
}

