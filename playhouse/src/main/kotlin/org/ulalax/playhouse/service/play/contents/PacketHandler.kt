package org.ulalax.playhouse.service.play.contents
import Logger
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.service.api.ApiException

class PacketHandler<S,A>(private val log: Logger) {

    private val messageMap = HashMap<Int, PacketCmd<S, A>>()
    suspend fun dispatch(stage: S, actor: A, packet: Packet){
        messageMap[packet.msgId]
            ?.execute(stage,actor,packet)
            ?:log.error("unregistered packet ${packet.msgId}",this::class.simpleName)
    }

    fun add(msgId:Int, cmd: PacketCmd<S, A>){
        if(messageMap.contains(msgId)){
            throw ApiException.DuplicateApiHandler("msgId:$msgId is already registered")
        }
        messageMap[msgId] = cmd
    }
}