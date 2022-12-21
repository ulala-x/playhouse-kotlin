package org.ulalax.playhouse.service.play.contents
import org.apache.logging.log4j.kotlin.logger
import org.ulalax.playhouse.protocol.Packet

class PacketHandler<S,A> {
    private val log = logger()
    private val messageMap = HashMap<String, PacketCmd<S, A>>()

    suspend fun dispatch(stage: S, actor: A, packet: Packet){
        messageMap[packet.msgName]
            ?.execute(stage,actor,packet)
            ?:log.error("unregisterd packet ${packet.msgName}")
    }

    fun add(msgName:String,cmd: PacketCmd<S, A>){
        messageMap[msgName] = cmd
    }
}