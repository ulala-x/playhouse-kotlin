package org.ulalax.playhouse.service.play.contents
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.service.api.ApiException

class PacketHandler<S,A> {

    private val messageMap = HashMap<Int, PacketCmd<S, A>>()
    suspend fun dispatch(stage: S, actor: A, packet: Packet){
        messageMap[packet.msgId]
            ?.execute(stage,actor,packet)
            ?:throw ApiException.NotRegisterApiMethod("msgId:${packet.msgId} is not registered")
    }

    fun add(msgId:Int, cmd: PacketCmd<S, A>){
        if(messageMap.contains(msgId)){
            throw ApiException.DuplicatedMessageIndex("msgId:$msgId is already registered")
        }
        messageMap[msgId] = cmd
    }
}