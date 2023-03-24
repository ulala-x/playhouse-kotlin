package org.ulalax.playhouse.communicator

import ConsoleLogger
import Logger
import org.ulalax.playhouse.service.CommonSender
import org.ulalax.playhouse.service.ServerSystem
import org.ulalax.playhouse.service.SystemPanel

class CommonOption {
    var port:Int = 0
    val redisIp:String = "localhost"
    var redisPort:Int = 6379
    var serviceId:String = ""
    lateinit var serverSystem:(SystemPanel, CommonSender) -> ServerSystem
    var requestTimeoutSec:Long = 5
    var showQps:Boolean = false
    var logger: Logger = ConsoleLogger()
}