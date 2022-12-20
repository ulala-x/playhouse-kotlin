package com.lifemmo.pl.base.service.session.network

import com.lifemmo.pl.base.protocol.ClientPacket
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import org.apache.logging.log4j.kotlin.logger

class WebSocketHandler(private val sessionPacketListener: SessionPacketListener) : ChannelInboundHandlerAdapter() {

    private val log = logger()
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        sessionPacketListener.onReceive(ctx.channel(),msg as ClientPacket)
    }

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        log.info{"connect"}
        sessionPacketListener.onConnect(ctx.channel())
    }

    override fun handlerRemoved(ctx: ChannelHandlerContext) {
        log.info{"disconnect"}
        sessionPacketListener.onDisconnect(ctx.channel())
    }

    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        if (evt is IdleStateEvent) {
            if (evt.state() == IdleState.READER_IDLE || evt.state() == IdleState.WRITER_IDLE) {
                sessionPacketListener.onDisconnect(ctx.channel())
                log.trace("client socket idle disconnect")
                ctx.close()
            }
        }
    }

//    override fun channelReadComplete(ctx: ChannelHandlerContext) {
//        super.channelReadComplete(ctx)
//    }
//
//    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
//        super.exceptionCaught(ctx, cause)
//    }
}