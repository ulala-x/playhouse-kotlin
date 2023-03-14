package org.ulalax.playhouse.communicator



interface StorageClient {
    fun updateServerInfo(serverInfo: XServerInfo)
    fun getServerList(endpoint:String):List<XServerInfo>
}

