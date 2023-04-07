package org.ulalax.playhouse.service.play.base

import LOG
import org.ulalax.playhouse.communicator.message.RoutePacket

class BaseStageCmdHandler() {

    private val maps = HashMap<Int, BaseStageCmd>()
    fun register(msgId: Int, baseStageCmd: BaseStageCmd){
        if(msgId in maps){
            throw RuntimeException("Already exist command : $msgId")
        }
        maps[msgId] = baseStageCmd
    }

    suspend fun dispatch(baseStage: BaseStage, request: RoutePacket) {
        val msgId = request.msgId()
        if(request.isBase()){
            if(maps.containsKey(msgId)){
                maps[msgId]?.execute(baseStage,request)
            }else{
                LOG.error ("not registered message : $msgId",this)
            }
        }else{
            LOG.error("Invalid packet : $msgId",this)
        }
    }
}