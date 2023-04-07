package org.ulalax.playhouse.service

import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.protocol.Common.BaseErrorCode

open class StageResult(open val errorCode: Short){
    fun isSuccess() = errorCode == BaseErrorCode.SUCCESS_VALUE.toShort()
}
data class CreateStageResult(override val errorCode: Short,
                            val stageId:Long,
                            val createStageRes: Packet
): StageResult(errorCode)

data class JoinStageResult(override val errorCode: Short,
                          val joinStageRes: Packet
) : StageResult(errorCode)
data class CreateJoinStageResult(override val errorCode:Short,
                                val isCreate:Boolean,
                                val createStageRes: Packet,
                                val joinStageRes: Packet
) : StageResult(errorCode)

