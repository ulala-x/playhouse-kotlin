package org.ulalax.playhouse.service.play.base.command

import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.service.play.PlayService
import org.ulalax.playhouse.service.play.base.BaseStage
import org.ulalax.playhouse.service.play.base.BaseStageCmd

class DisconnectNoticeCmd(override val playService: PlayService) : BaseStageCmd {
    override suspend fun execute(baseStage: BaseStage, routePacket: RoutePacket) {
        val accountId = routePacket.accountId()
        baseStage.onDisconnect(accountId)
    }
}