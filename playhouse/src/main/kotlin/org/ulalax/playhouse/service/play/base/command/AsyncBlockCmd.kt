package org.ulalax.playhouse.service.play.base.command

import org.ulalax.playhouse.communicator.message.AsyncBlockPacket
import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.service.play.PlayProcessor
import org.ulalax.playhouse.service.play.base.BaseStage
import org.ulalax.playhouse.service.play.base.BaseStageCmd

class AsyncBlockCmd(override val playProcessor: PlayProcessor) : BaseStageCmd {

    override suspend fun execute(baseStage: BaseStage, routePacket: RoutePacket) {
        @Suppress("UNCHECKED_CAST")
        val asyncBlock = routePacket as AsyncBlockPacket<Any>
        asyncBlock.asyncPostCallback(asyncBlock.result)
    }
}