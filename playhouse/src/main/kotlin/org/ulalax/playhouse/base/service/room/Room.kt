package org.ulalax.playhouse.base.service.room

import org.ulalax.playhouse.base.protocol.Packet
import org.ulalax.playhouse.base.protocol.ReplyPacket
import org.ulalax.playhouse.base.service.RoomSender

interface Room<U:User> {
    val roomSender: RoomSender

    suspend fun onCreate(packet: Packet): ReplyPacket
    suspend fun onJoinRoom(user: U, packet: Packet): ReplyPacket
    suspend fun onDispatch(user: U, packet: Packet)
    suspend fun onDisconnect(user: U)
    suspend fun onPostCreate()
    suspend fun onPostJoinRoom(user: U)


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
