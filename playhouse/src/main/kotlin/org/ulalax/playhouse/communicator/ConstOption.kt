package org.ulalax.playhouse.communicator

import org.ulalax.playhouse.ConsoleLogger
import org.ulalax.playhouse.Logger

object ConstOption {

    val THREAD_SLEEP:Long = 50
    val ADDRESS_RESOLVER_PERIOD:Long = 1000
    val ADDRESS_RESOLVER_INITIAL_DELAY:Long = 3000
    val REDIS_CACHE_KEY:String = "playhouse_serverinfos"

    val logger:Logger = ConsoleLogger()
}