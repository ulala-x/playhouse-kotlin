package org.ulalax.playhouse.communicator

import ConsoleLogger
import Logger

object ConstOption {

    const val THREAD_SLEEP:Long = 10
    const val ADDRESS_RESOLVER_PERIOD:Long = 1000
    const val ADDRESS_RESOLVER_INITIAL_DELAY:Long = 3000
    const val REDIS_CACHE_KEY:String = "playhouse_serverinfos"

    const val MAX_PACKET_SIZE = 65535
    const val HEADER_SIZE = 10

    val logger: Logger = ConsoleLogger()
}