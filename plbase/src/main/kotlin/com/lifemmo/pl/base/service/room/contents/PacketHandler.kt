package com.lifemmo.pl.base.service.room.contents
import org.apache.logging.log4j.kotlin.logger
import com.lifemmo.pl.base.protocol.Packet

class PacketHandler<R,U> {
    private val log = logger()
    private val messageMap = HashMap<String, PacketCmd<R, U>>()

    suspend fun dispatch(room: R, user: U, packet: Packet){
        messageMap[packet.msgName]
            ?.execute(room,user,packet)
            ?:log.error("unregisterd packet ${packet.msgName}")
    }

    fun add(msgName:String,cmd: PacketCmd<R, U>){
        messageMap[msgName] = cmd
    }
}