package org.ulalax.playhouse.service.play.base

import org.ulalax.playhouse.service.StageSender
import org.ulalax.playhouse.service.play.Stage
import org.ulalax.playhouse.service.play.Actor
import org.ulalax.playhouse.service.play.ActorSender

class ElementConfigurator {
    val rooms = HashMap<String,(StageSender)-> Stage<Actor>>()
    var users = HashMap<String,(ActorSender)-> Actor>()

    fun register(StageType:String, stage:(StageSender)-> Stage<out Actor>, actor:(ActorSender)-> Actor){
        @Suppress("UNCHECKED_CAST")
        rooms[StageType] = stage as (StageSender) -> Stage<Actor>
        users[StageType] = actor
    }
}

//class ElementConfigurator {
//    val rooms = HashMap<String, KClass<out Room<out User>>>()
//    var users = HashMap<String,KClass<out User>>()
//
//    fun register(StageType:String,roomClazz:KClass<out Room<out User>> ,userClazz:KClass<out User>){
//        rooms[StageType] = roomClazz
//        users[StageType] = userClazz
//    }
//}