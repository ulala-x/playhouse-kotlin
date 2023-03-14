package org.ulalax.playhouse.service.api

interface ApiCallBack {
    fun onDisconnect(accountId:Long,sessionInfo:String)
}