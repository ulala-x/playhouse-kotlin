package org.ulalax.playhouse.communicator

import java.net.ServerSocket


object TestHelper {
    fun findFreePort(): Int {
        ServerSocket(0).use { socket ->
            socket.reuseAddress = true
            return socket.localPort
        }
    }
}