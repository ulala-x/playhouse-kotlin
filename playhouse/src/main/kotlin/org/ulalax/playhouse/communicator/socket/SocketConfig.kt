package org.ulalax.playhouse.communicator.socket

class SocketConfig {
    val backLog:Int = 1000
    val linger:Int = 0
    val sendBufferSize:Int = 1024*1024
    val receiveBufferSize:Int = 1024*1024
    val sendHighWatermark:Int = 1000000
    val receiveHighWatermark:Int = 1000000
}