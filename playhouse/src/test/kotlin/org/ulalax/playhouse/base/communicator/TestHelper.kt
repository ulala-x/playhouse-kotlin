package org.ulalax.playhouse.base.communicator

import java.net.ServerSocket


object TestHelper {
    fun findFreePort(): Int {
        ServerSocket(0).use { socket ->
            socket.reuseAddress = true
            return socket.localPort
        }
    }
}