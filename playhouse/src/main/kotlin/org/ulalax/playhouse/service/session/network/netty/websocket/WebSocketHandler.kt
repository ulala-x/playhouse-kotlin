package org.ulalax.playhouse.service.session.network.netty.websocket

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import org.ulalax.playhouse.LOG
import org.ulalax.playhouse.communicator.message.ClientPacket
import org.ulalax.playhouse.service.session.network.netty.SessionPacketListener

class WebSocketHandler(private val sessionPacketListener: SessionPacketListener) : ChannelInboundHandlerAdapter() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        sessionPacketListener.onReceive(ctx.channel(),msg as ClientPacket)
    }

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        LOG.info("connect",this::class.simpleName)
        sessionPacketListener.onConnect(ctx.channel())
    }

    override fun handlerRemoved(ctx: ChannelHandlerContext) {
        LOG.info("disconnect",this::class.simpleName)
        sessionPacketListener.onDisconnect(ctx.channel())
        ctx.close()
    }

    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        if (evt is IdleStateEvent) {
            if (evt.state() == IdleState.READER_IDLE || evt.state() == IdleState.WRITER_IDLE) {
                sessionPacketListener.onDisconnect(ctx.channel())
                LOG.trace("client socket idle disconnect",this::class.simpleName)
                ctx.close()
            }
        }
    }

}