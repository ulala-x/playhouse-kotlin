package org.ulalax.playhouse.communicator.socket

object PlaySocketFactory {
    fun createPlaySocket(socketConfig: SocketConfig,id:String):PlaySocket{
        return ZmqJPlaySocket(socketConfig,id)
    }

}