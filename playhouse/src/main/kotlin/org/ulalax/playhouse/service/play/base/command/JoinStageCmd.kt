package org.ulalax.playhouse.service.play.base.command

import com.google.protobuf.ByteString
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.service.play.PlayProcessor
import org.ulalax.playhouse.service.play.base.BaseStage
import org.ulalax.playhouse.service.play.base.BaseStageCmd
import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.communicator.message.ReplyPacket
import org.ulalax.playhouse.protocol.Server.*

class JoinStageCmd(override val playService: PlayProcessor): BaseStageCmd {
    override suspend fun execute(baseStage: BaseStage, routePacket: RoutePacket) {

        val request = JoinStageReq.parseFrom(routePacket.data())
        val accountId = routePacket.accountId()
        val sessionEndpoint = request.sessionEndpoint
        val sid = request.sid
        val packet = Packet(request.payloadId,request.payload)
        val apiEndpoint = routePacket.routeHeader.from

        val outcome = baseStage.join(accountId,sessionEndpoint,sid,apiEndpoint,packet)
        val response = JoinStageRes.newBuilder()
            .setPayload(ByteString.copyFrom(outcome.data()))
            .setPayloadId(outcome.msgId)
            .build()

        baseStage.reply(ReplyPacket(outcome.errorCode,response))

        if(outcome.isSuccess()){
            baseStage.onPostJoinRoom(accountId)
        }
    }
}