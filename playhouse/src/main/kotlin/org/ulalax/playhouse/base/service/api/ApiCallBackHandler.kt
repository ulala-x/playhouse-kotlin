package org.ulalax.playhouse.base.service.api

interface ApiCallBackHandler {
    fun onDisconnect(accountId:Long,sessionInfo:String)
}