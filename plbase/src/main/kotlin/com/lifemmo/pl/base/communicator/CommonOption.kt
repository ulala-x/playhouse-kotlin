package com.lifemmo.pl.base.communicator

import com.lifemmo.pl.base.service.BaseSender
import com.lifemmo.pl.base.service.ServerSystem
import com.lifemmo.pl.base.service.SystemPanel

class CommonOption {
    var port:Int = 0
    val redisIp:String = "localhost"
    var redisPort:Int = 6379
    var serviceId:String = ""
    lateinit var serverSystem:(SystemPanel,BaseSender) -> ServerSystem
    var requestTimeoutSec:Long = 5
    var showQps:Boolean = false


// class Builder{
//     private var port:Int = 0
//     private var usePublicIp:Boolean = false
//     private var targetRedisPort:Int = 0
//
//     fun port(port:Int) = apply { this.port = port }
//     fun usePublicIp() = apply { this.usePublicIp =true }
//     fun targetRedisPort(port: Int) = apply { this.targetRedisPort = port }
//     fun build() = apply {CommonOption(port,usePublicIp,targetRedisPort)}
// }
}