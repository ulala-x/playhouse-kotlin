package org.ulalax.playhouse.service.play.base.command

import com.google.protobuf.ByteString
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.service.play.PlayProcessor
import org.ulalax.playhouse.service.play.base.BaseStage
import org.ulalax.playhouse.service.play.base.BaseStageCmd
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.communicator.message.ReplyPacket
import org.ulalax.playhouse.protocol.Common.BaseErrorCode
import org.ulalax.playhouse.protocol.Server.*

class CreateStageCmd(override val playProcessor: PlayProcessor): BaseStageCmd {
    override suspend fun execute(baseStage: BaseStage, routePacket: RoutePacket) {

        val createStageReq = CreateStageReq.parseFrom(routePacket.data())
        val packet = Packet(createStageReq.payloadId,createStageReq.payload)
        val stageType = createStageReq.stageType

        if(!playProcessor.isValidType(stageType)){
            playProcessor.errorReply(routePacket.routeHeader,
                BaseErrorCode.STAGE_TYPE_IS_INVALID_VALUE.toShort())
            return
        }

        val outcome  = baseStage.create(stageType,packet)
        val stageId = baseStage.stageId()

        if(!outcome.isSuccess()){
            this.playProcessor.removeRoom(stageId)
        }

        val res = CreateStageRes.newBuilder()
            .setPayload(ByteString.copyFrom(outcome.data()))
            .setPayloadId(outcome.msgId).build()

        baseStage.reply(ReplyPacket(outcome.errorCode,res))

        if(outcome.isSuccess()){
            baseStage.onPostCreate()
        }
    }
}