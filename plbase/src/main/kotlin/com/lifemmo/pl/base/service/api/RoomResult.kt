package com.lifemmo.pl.base.service.api

import com.lifemmo.pl.base.Plcommon.BaseErrorCode
import com.lifemmo.pl.base.protocol.Packet
import com.lifemmo.pl.base.service.room.Room

open class RoomResult(open val errorCode: Int){
    fun isSuccess() = errorCode == BaseErrorCode.SUCCESS_VALUE
}
data class CreateRoomResult(override val errorCode: Int,
                            val roomId:Long,
                            val createRoomRes: Packet):RoomResult(errorCode)

data class JoinRoomResult(override val errorCode: Int,
                          val joinRoomRes:Packet) :RoomResult(errorCode)
data class CreateJoinRoomResult(override val errorCode:Int,
                                val isCreate:Boolean,
                                val createRoomRes: Packet,
                                val joinRoomRes:Packet) :RoomResult(errorCode)

