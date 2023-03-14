package org.ulalax.playhouse.service.play.contents
import org.ulalax.playhouse.Logger
import org.ulalax.playhouse.communicator.message.Packet

class PacketHandler<S,A>(private val log:Logger) {

    private val messageMap = HashMap<String, PacketCmd<S, A>>()
    suspend fun dispatch(stage: S, actor: A, packet: Packet){
        messageMap[packet.msgName]
            ?.execute(stage,actor,packet)
            ?:log.error("unregistered packet ${packet.msgName}",this::class.simpleName)
    }

    fun add(msgName:String,cmd: PacketCmd<S, A>){
        messageMap[msgName] = cmd
    }
}