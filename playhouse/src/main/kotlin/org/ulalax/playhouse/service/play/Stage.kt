package org.ulalax.playhouse.service.play

import org.ulalax.playhouse.protocol.Packet
import org.ulalax.playhouse.protocol.ReplyPacket
import org.ulalax.playhouse.service.StageSender

interface Stage<A: Actor> {
    val stageSender: StageSender

    suspend fun onCreate(packet: Packet): ReplyPacket
    suspend fun onJoinStage(actor: A, packet: Packet): ReplyPacket
    suspend fun onDispatch(actor: A, packet: Packet)
    suspend fun onDisconnect(actor: A)
    suspend fun onPostCreate()
    suspend fun onPostJoinStage(actor: A)


}

//fun <T> addAll(list1:MutableList<in T>,list2: MutableList<out T>){
//    for( elem in list2)list1.add(elem)
//}
//
//val la:MutableList<Any> = mutableListOf(1,"d")
//val ls = mutableListOf("string", "a")
//
//fun test(){
//    addAll(la,ls)
//}
