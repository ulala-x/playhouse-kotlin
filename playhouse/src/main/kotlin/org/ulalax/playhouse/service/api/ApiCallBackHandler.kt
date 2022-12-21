package org.ulalax.playhouse.service.api

interface ApiCallBackHandler {
    fun onDisconnect(accountId:Long,sessionInfo:String)
}