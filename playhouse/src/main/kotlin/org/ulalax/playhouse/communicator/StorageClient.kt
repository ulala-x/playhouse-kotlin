package org.ulalax.playhouse.communicator



interface IStorageClient {
    fun updateServerInfo(serverInfo: ServerInfo)
    fun getServerList(endpoint:String):List<ServerInfo>
}

