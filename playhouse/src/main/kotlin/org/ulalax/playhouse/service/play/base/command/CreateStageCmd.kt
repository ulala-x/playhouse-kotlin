package org.ulalax.playhouse.service.play.base.command

import com.google.protobuf.ByteString
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.protocol.Packet
import org.ulalax.playhouse.protocol.ReplyPacket
import org.ulalax.playhouse.service.play.PlayService
import org.ulalax.playhouse.service.play.base.BaseStage
import org.ulalax.playhouse.service.play.base.BaseStageCmd
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.logging.log4j.kotlin.logger
import org.ulalax.playhouse.ErrorCode
import org.ulalax.playhouse.protocol.Server.*

class CreateStageCmd(override val playService: PlayService): BaseStageCmd {
    private val log = logger()
    override suspend fun execute(baseStage: BaseStage, routePacket: RoutePacket) {

        val createStageReq = CreateStageReq.parseFrom(routePacket.buffer())
        val packet = Packet(createStageReq.payloadName,createStageReq.payload)
        val StageType = createStageReq.stageType

        try {

            if(!playService.isValidType(StageType)){
                playService.errorReply(routePacket.routeHeader,
                    ErrorCode.STAGE_TYPE_IS_INVALID)
                return
            }

            val outcome  = baseStage.create(StageType,packet)
            val stageId = baseStage.stageId()

            if(!outcome.isSuccess()){
                this.playService.removeRoom(stageId)
            }

            val res = CreateStageRes.newBuilder()
                .setStageId(stageId)
                .setPayload(ByteString.copyFrom(outcome.buffer()))
                .setPayloadName(outcome.msgName).build()

            baseStage.reply(ReplyPacket(outcome.errorCode,res))

            if(outcome.isSuccess()){
                baseStage.onPostCreate()
            }

        }catch (e:Exception){
            log.error(ExceptionUtils.getStackTrace(e))
        }
    }
}