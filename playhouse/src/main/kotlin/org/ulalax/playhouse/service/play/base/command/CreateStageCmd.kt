package org.ulalax.playhouse.service.play.base.command

import com.google.protobuf.ByteString
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.service.play.PlayService
import org.ulalax.playhouse.service.play.base.BaseStage
import org.ulalax.playhouse.service.play.base.BaseStageCmd
import org.apache.commons.lang3.exception.ExceptionUtils
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.communicator.message.ReplyPacket
import org.ulalax.playhouse.protocol.Common.BaseErrorCode
import org.ulalax.playhouse.protocol.Server.*

class CreateStageCmd(override val playService: PlayService): BaseStageCmd {
    override suspend fun execute(baseStage: BaseStage, routePacket: RoutePacket) {

        val createStageReq = CreateStageReq.parseFrom(routePacket.data())
        val packet = Packet(createStageReq.payloadName,createStageReq.payload)
        val stageType = createStageReq.stageType

        if(!playService.isValidType(stageType)){
            playService.errorReply(routePacket.routeHeader,
                BaseErrorCode.STAGE_TYPE_IS_INVALID.number)
            return
        }

        val outcome  = baseStage.create(stageType,packet)
        val stageId = baseStage.stageId()

        if(!outcome.isSuccess()){
            this.playService.removeRoom(stageId)
        }

        val res = CreateStageRes.newBuilder()
            .setStageId(stageId)
            .setPayload(ByteString.copyFrom(outcome.data()))
            .setPayloadName(outcome.msgName).build()

        baseStage.reply(ReplyPacket(outcome.errorCode,res))

        if(outcome.isSuccess()){
            baseStage.onPostCreate()
        }
    }
}