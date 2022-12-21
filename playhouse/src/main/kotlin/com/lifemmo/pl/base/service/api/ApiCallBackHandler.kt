package com.lifemmo.pl.base.service.api

interface ApiCallBackHandler {
    fun onDisconnect(accountId:Long,sessionInfo:String)
}