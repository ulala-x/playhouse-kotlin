package org.ulalax.playhouse.communicator



interface StorageClient {
    fun updateServerInfo(serverInfo: ServerInfoImpl)
    fun getServerList(endpoint:String):List<ServerInfoImpl>
}

