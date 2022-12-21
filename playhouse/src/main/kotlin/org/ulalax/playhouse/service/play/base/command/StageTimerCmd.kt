package org.ulalax.playhouse.service.play.base.command

import org.ulalax.playhouse.communicator.message.RoutePacket
import org.ulalax.playhouse.service.play.PlayService
import org.ulalax.playhouse.service.play.base.BaseStage
import org.ulalax.playhouse.service.play.base.BaseStageCmd
import org.apache.logging.log4j.kotlin.logger

class StageTimerCmd(override val playService: PlayService): BaseStageCmd {
    private val log = logger()
    override suspend fun execute(baseStage: BaseStage, routePacket: RoutePacket) {

        val timerCallback = routePacket.timerCallback
        val timerId = routePacket.timerId
        if(baseStage.hasTimer(timerId)){
            timerCallback()
        }else{
            log.warn("timer already canceled stageId:${baseStage.stageId()}, timerId:$timerId")
        }
    }


}