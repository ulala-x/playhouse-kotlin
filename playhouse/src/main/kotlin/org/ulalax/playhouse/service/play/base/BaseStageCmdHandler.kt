package org.ulalax.playhouse.service.play.base

import org.ulalax.playhouse.communicator.message.RoutePacket
import org.apache.logging.log4j.kotlin.logger

class BaseStageCmdHandler {
    private val logger = logger()
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
                logger.error { "not registered message : $msgName" }
            }
        }else{
            logger.error("Invalid packet : $msgName")
        }
    }
}