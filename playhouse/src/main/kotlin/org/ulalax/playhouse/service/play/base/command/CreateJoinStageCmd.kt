package org.ulalax.playhouse.service.play.base.command

import com.google.protobuf.ByteString
import org.ulalax.playhouse.ErrorCode
import org.ulalax.playhouse.protocol.Server.*
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.protocol.Packet
import org.ulalax.playhouse.protocol.ReplyPacket
import org.ulalax.playhouse.service.play.PlayService
import org.ulalax.playhouse.service.play.base.BaseStage
import org.ulalax.playhouse.service.play.base.BaseStageCmd

class CreateJoinStageCmd(override val playService: PlayService) : BaseStageCmd {
    override suspend fun execute(baseStage: BaseStage, routePacket: RoutePacket) {
        val request = CreateJoinStageReq.parseFrom(routePacket.buffer())
        val createStagePacket = Packet(request.createPayloadName,request.createPayload)
        val StageType = request.stageType
        val joinStagePacket = Packet(request.joinPayloadName,request.joinPayload)
        val accountId = routePacket.accountId()
        val stageId = routePacket.stageId()
        val sessionEndpoint = request.sessionEndpoint
        val sid = request.sid
        val apiEndpoint = routePacket.routeHeader.from

        var createReply: ReplyPacket
        val responseBuilder = CreateJoinStageRes.newBuilder()

        if(!playService.isValidType(StageType)){
            playService.errorReply(routePacket.routeHeader, ErrorCode.STAGE_TYPE_IS_INVALID)
            return
        }

        if(!baseStage.isCreated){
            createReply = baseStage.create(StageType,createStagePacket)
            responseBuilder
                .setCreatePayloadName(createReply.msgName)
                .setCreatePayload(ByteString.copyFrom(createReply.buffer()))

            if(!createReply.isSuccess()){
                playService.removeRoom(stageId)
                val response = CreateJoinStageRes.newBuilder()
                                    .setCreatePayloadName(createReply.msgName)
                                    .setCreatePayload(ByteString.copyFrom(createReply.buffer())).build()

                baseStage.reply(ReplyPacket(createReply.errorCode,response))
                return
            }else{
                baseStage.onPostCreate()
                responseBuilder.isCreated = true
            }

        }



        val joinReply = baseStage.join(accountId,sessionEndpoint,sid,apiEndpoint,joinStagePacket)
        val response = responseBuilder
            .setJoinPayloadName(joinReply.msgName)
            .setJoinPayload(ByteString.copyFrom(joinReply.buffer()))
            .build()

        baseStage.reply(ReplyPacket(joinReply.errorCode,response))

        if(joinReply.isSuccess()){
            baseStage.onPostJoinRoom(accountId)
        }

    }
}