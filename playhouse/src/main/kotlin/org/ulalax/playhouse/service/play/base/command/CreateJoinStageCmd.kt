package org.ulalax.playhouse.service.play.base.command

import com.google.protobuf.ByteString
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.communicator.message.ReplyPacket
import org.ulalax.playhouse.protocol.Server.*
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.protocol.Common.BaseErrorCode
import org.ulalax.playhouse.service.play.PlayProcessor
import org.ulalax.playhouse.service.play.base.BaseStage
import org.ulalax.playhouse.service.play.base.BaseStageCmd

class CreateJoinStageCmd(override val playProcessor: PlayProcessor) : BaseStageCmd {
    override suspend fun execute(baseStage: BaseStage, routePacket: RoutePacket) {
        val request = CreateJoinStageReq.parseFrom(routePacket.data())
        val createStagePacket = Packet(request.createPayloadId,request.createPayload)
        val stageType = request.stageType
        val joinStagePacket = Packet(request.joinPayloadId,request.joinPayload)
        val accountId = routePacket.accountId()
        val stageId = routePacket.stageId()
        val sessionEndpoint = request.sessionEndpoint
        val sid = request.sid
        val apiEndpoint = routePacket.routeHeader.from

        var createReply: ReplyPacket
        val responseBuilder = CreateJoinStageRes.newBuilder()

        if(!playProcessor.isValidType(stageType)){
            playProcessor.errorReply(routePacket.routeHeader, BaseErrorCode.STAGE_TYPE_IS_INVALID_VALUE.toShort())
            return
        }

        if(!baseStage.isCreated){
            createReply = baseStage.create(stageType,createStagePacket)
            responseBuilder
                .setCreatePayloadId(createReply.msgId)
                .setCreatePayload(ByteString.copyFrom(createReply.data()))

            if(!createReply.isSuccess()){
                playProcessor.removeRoom(stageId)
                baseStage.reply(ReplyPacket(createReply.errorCode,responseBuilder.build()))
                return
            }else{
                baseStage.onPostCreate()
                responseBuilder.isCreated = true
            }
        }

        val joinResult = baseStage.join(accountId,sessionEndpoint,sid,apiEndpoint,joinStagePacket)
        val joinReply = joinResult.first
        val stageIndex = joinResult.second

        responseBuilder
            .setJoinPayloadId(joinReply.msgId)
            .setJoinPayload(ByteString.copyFrom(joinReply.data()))
            .setStageIdx(stageIndex)

        baseStage.reply(ReplyPacket(joinReply.errorCode,responseBuilder.build()))

        if(joinReply.isSuccess()){
            baseStage.onPostJoinRoom(accountId)
        }
    }
}