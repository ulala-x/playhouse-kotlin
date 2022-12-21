package com.lifemmo.pl.base.service.room.base

import com.lifemmo.pl.base.communicator.message.RoutePacket
import com.lifemmo.pl.base.service.room.RoomSenderImpl
import org.apache.logging.log4j.kotlin.logger

class BaseRoomCmdHandler {
    private val logger = logger()
    private val maps = HashMap<String, BaseRoomCmd>()
    fun register(msgName: String, baseRoomCmd: BaseRoomCmd){
        if(msgName in maps){
            throw RuntimeException("Already exist command : $msgName")
        }
        maps[msgName] = baseRoomCmd
    }

    suspend fun dispatch(baseRoom: BaseRoom, request: RoutePacket) {
        val msgName = request.msgName()
        if(request.isBase()){
            if(maps.containsKey(msgName)){
                maps[msgName]?.execute(baseRoom,request)
            }else{
                logger.error { "not registered message : $msgName" }
            }
        }else{
            logger.error("Invalid packet : $msgName")
        }
    }
}