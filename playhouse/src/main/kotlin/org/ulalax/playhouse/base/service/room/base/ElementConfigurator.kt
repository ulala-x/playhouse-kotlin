package org.ulalax.playhouse.base.service.room.base

import org.ulalax.playhouse.base.service.RoomSender
import org.ulalax.playhouse.base.service.room.Room
import org.ulalax.playhouse.base.service.room.User
import org.ulalax.playhouse.base.service.room.UserSender
import kotlin.reflect.KClass

class ElementConfigurator {
    val rooms = HashMap<String,(RoomSender)->Room<User>>()
    var users = HashMap<String,(UserSender)->User>()

    fun register(roomType:String,room:(RoomSender)->Room<out User>,user:(UserSender)->User){
        @Suppress("UNCHECKED_CAST")
        rooms[roomType] = room as (RoomSender) -> Room<User>
        users[roomType] = user
    }
}

//class ElementConfigurator {
//    val rooms = HashMap<String, KClass<out Room<out User>>>()
//    var users = HashMap<String,KClass<out User>>()
//
//    fun register(roomType:String,roomClazz:KClass<out Room<out User>> ,userClazz:KClass<out User>){
//        rooms[roomType] = roomClazz
//        users[roomType] = userClazz
//    }
//}