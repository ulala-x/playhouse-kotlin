package org.ulalax.playhouse.service.api

import org.ulalax.playhouse.protocol.Common.BaseErrorCode
import org.ulalax.playhouse.protocol.Packet

open class StageResult(open val errorCode: Int){
    fun isSuccess() = errorCode == BaseErrorCode.SUCCESS_VALUE
}
data class CreateStageResult(override val errorCode: Int,
                            val stageId:Long,
                            val createStageRes: Packet
): StageResult(errorCode)

data class JoinStageResult(override val errorCode: Int,
                          val joinStageRes: Packet
) : StageResult(errorCode)
data class CreateJoinStageResult(override val errorCode:Int,
                                val isCreate:Boolean,
                                val createStageRes: Packet,
                                val joinStageRes: Packet
) : StageResult(errorCode)

