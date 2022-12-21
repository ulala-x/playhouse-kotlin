package org.ulalax.playhouse.client

import org.ulalax.playhouse.client.network.BasePacketListener
import org.ulalax.playhouse.protocol.ClientPacket
import io.netty.channel.Channel
import org.apache.logging.log4j.kotlin.logger

class BasePacketListener(private val requestCache: RequestCache,
                         private val clientPacketListener: ClientPacketListener
) :
    BasePacketListener {
    private val log = logger()

    override fun onConnect(channel: Channel) {
        log.info("connected")
    }

    override fun onReceive(channel: Channel, clientPacket: ClientPacket) {

        clientPacket.use {
            log.debug("onReceive:${clientPacket.msgName()},${clientPacket.header.msgSeq}")

            val msgSeq = clientPacket.header.msgSeq
            if (msgSeq != 0) {
                requestCache.onReply(clientPacket)
                return
            }
            val packet = clientPacket.toPacket()

            packet.use {

                clientPacketListener.onReceive(clientPacket.serviceId(), packet)
            }

        }
    }

     override fun onDisconnect(channel: Channel) {
        log.info("Disconnected")
    }

}