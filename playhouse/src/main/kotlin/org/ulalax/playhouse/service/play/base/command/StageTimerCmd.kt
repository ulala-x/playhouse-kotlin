package org.ulalax.playhouse.service.play.base.command

import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.service.play.PlayProcessor
import org.ulalax.playhouse.service.play.base.BaseStage
import org.ulalax.playhouse.service.play.base.BaseStageCmd
import LOG

class StageTimerCmd(override val playService: PlayProcessor): BaseStageCmd {

    override suspend fun execute(baseStage: BaseStage, routePacket: RoutePacket) {

        val timerCallback = routePacket.timerCallback
        val timerId = routePacket.timerId
        if(baseStage.hasTimer(timerId)){
            timerCallback()
        }else{
            LOG.warn("timer already canceled stageId:${baseStage.stageId()}, timerId:$timerId",this)
        }
    }


}