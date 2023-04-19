package org.ulalax.playhouse.client

import org.ulalax.playhouse.client.network.BasePacketListener
import io.netty.channel.Channel
import org.ulalax.playhouse.client.network.message.ClientPacket

class BasePacketListener(private val requestCache: RequestCache,
                         private val apiPacketListener: ApiPacketListener,
                         private val stagePacketListener: StagePacketListener,
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
            val serviceId = clientPacket.serviceId()
            val stageIndex = clientPacket.header.stageIndex
            if(stageIndex > 0){
                stagePacketListener.onReceive(serviceId,stageIndex.toInt(),it)
            }else{
                apiPacketListener.onReceive(serviceId,it)
            }
        }
    }

    override fun onDisconnect(channel: Channel) {
        LOG.info("Disconnected",this)
    }
}

