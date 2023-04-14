package org.ulalax.playhouse.client

import org.ulalax.playhouse.client.network.BasePacketListener
import io.netty.channel.Channel
import org.ulalax.playhouse.client.network.message.ClientPacket

class BasePacketListener(private val requestCache: RequestCache,
                         private val clientPacketListener: ClientPacketListener,

) : BasePacketListener {


    override fun onConnect(channel: Channel) {
        LOG.info("connected",this)
    }

    override fun onReceive(channel: Channel, clientPacket: ClientPacket) {

        clientPacket.use {
            LOG.debug("onReceive - from server:${clientPacket.msgId()},${clientPacket.header.msgSeq}",this)
            val msgSeq = clientPacket.header.msgSeq
            if(msgSeq > 0){
                requestCache.onReply(clientPacket)
                return
            }
        }

        val packet = clientPacket.toPacket()
        packet.use {
            clientPacketListener.onReceive(
                TargetId(clientPacket.serviceId(),clientPacket.header.stageIndex.toInt()),
                packet
            )
        }
    }

    override fun onDisconnect(channel: Channel) {
        LOG.info("Disconnected",this)
    }
}

