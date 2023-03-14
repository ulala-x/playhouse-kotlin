package org.ulalax.playhouse.service.play

import org.ulalax.playhouse.communicator.message.Packet
import org.ulalax.playhouse.communicator.message.ReplyPacket
import org.ulalax.playhouse.service.StageSender

class ActorStub(override val actorSender: ActorSender) : Actor {
    override fun onCreate() {
    }

    override fun onDestroy() {
    }

}

class StageStub(override val stageSender: StageSender) : Stage<ActorStub> {
    override suspend fun onCreate(packet: Packet): ReplyPacket {
        return ReplyPacket(0,packet.msgName,packet.movePayload())
    }

    override suspend fun onJoinStage(actor: ActorStub, packet: Packet): ReplyPacket {
        return ReplyPacket(0,packet.msgName,packet.movePayload())
    }

    override suspend fun onDispatch(actor: ActorStub, packet: Packet) {
    }

    override suspend fun onDisconnect(actor: ActorStub) {
    }

    override suspend fun onPostCreate() {
    }

    override suspend fun onPostJoinStage(actor: ActorStub) {
    }
}