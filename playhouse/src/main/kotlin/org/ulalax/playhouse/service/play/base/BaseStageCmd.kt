package org.ulalax.playhouse.service.play.base

import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.service.play.PlayProcessor

interface BaseStageCmd {
    val playService: PlayProcessor
    suspend fun execute(baseStage: BaseStage, routePacket: RoutePacket)
}