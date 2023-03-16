package org.ulalax.playhouse.service.play.base

import org.ulalax.playhouse.LOG
import org.ulalax.playhouse.communicator.message.RoutePacket

class BaseStageCmdHandler() {

    private val maps = HashMap<String, BaseStageCmd>()
    fun register(msgName: String, baseStageCmd: BaseStageCmd){
        if(msgName in maps){
            throw RuntimeException("Already exist command : $msgName")
        }
        maps[msgName] = baseStageCmd
    }

    suspend fun dispatch(baseStage: BaseStage, request: RoutePacket) {
        val msgName = request.msgName()
        if(request.isBase()){
            if(maps.containsKey(msgName)){
                maps[msgName]?.execute(baseStage,request)
            }else{
                LOG.error ("not registered message : $msgName",this)
            }
        }else{
            LOG.error("Invalid packet : $msgName",this)
        }
    }
}