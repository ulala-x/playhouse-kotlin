package org.ulalax.playhouse.base.service.api

import org.ulalax.playhouse.Common.BaseErrorCode
import org.ulalax.playhouse.base.protocol.Packet

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

