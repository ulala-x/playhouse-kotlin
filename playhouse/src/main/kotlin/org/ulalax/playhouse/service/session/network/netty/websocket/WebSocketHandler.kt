package org.ulalax.playhouse.service.session.network.netty.websocket

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import LOG
import org.ulalax.playhouse.communicator.message.ClientPacket
import org.ulalax.playhouse.service.session.network.netty.SessionListener

class WebSocketHandler(private val sessionPacketListener: SessionListener) : ChannelInboundHandlerAdapter() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        sessionPacketListener.onReceive(ctx.channel(),msg as ClientPacket)
    }

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        LOG.info("connect",this)
        sessionPacketListener.onConnect(ctx.channel())
    }

    override fun handlerRemoved(ctx: ChannelHandlerContext) {
        LOG.info("disconnect",this)
        sessionPacketListener.onDisconnect(ctx.channel())
        ctx.close()
    }

    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        if (evt is IdleStateEvent) {
            if (evt.state() == IdleState.READER_IDLE || evt.state() == IdleState.WRITER_IDLE) {
                sessionPacketListener.onDisconnect(ctx.channel())
                LOG.trace("client socket idle disconnect",this)
                ctx.close()
            }
        }
    }

}