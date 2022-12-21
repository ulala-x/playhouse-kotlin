package com.lifemmo.pl.base.communicator



interface StorageClient {
    fun updateServerInfo(serverInfo: ServerInfo)
    fun getServerList(endpoint:String):List<ServerInfo>
}

