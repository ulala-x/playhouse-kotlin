package org.ulalax.playhouse.service.play.base

import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.service.play.PlayService

interface BaseStageCmd {
    val playService: PlayService
    suspend fun execute(baseStage: BaseStage, routePacket: RoutePacket)
}