package org.ulalax.playhouse.client.network.websocket

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import org.apache.logging.log4j.kotlin.logger
import org.ulalax.playhouse.client.network.BasePacketListener
import org.ulalax.playhouse.client.network.message.ClientPacket

class WebSocketHandler(private val basePacketListener: BasePacketListener) : ChannelInboundHandlerAdapter() {

    private val log = logger()

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        //super.channelRead(ctx, msg)
        basePacketListener.onReceive(ctx.channel(),msg as ClientPacket)
    }

    override fun handlerAdded(ctx: ChannelHandlerContext) {

        basePacketListener.onConnect(ctx.channel())
    }

    override fun handlerRemoved(ctx: ChannelHandlerContext) {
        basePacketListener.onDisconnect(ctx.channel())
    }

    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        if (evt is IdleStateEvent) {
            if (evt.state() == IdleState.READER_IDLE || evt.state() == IdleState.WRITER_IDLE) {
                basePacketListener.onDisconnect(ctx.channel())
                log.debug("client socket idle disconnect")
                ctx.close()
            }
        }
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        super.channelReadComplete(ctx)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        super.exceptionCaught(ctx, cause)
    }
}