package org.ulalax.playhouse.service.session.network.netty.websocket

import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.websocketx.*
import org.ulalax.playhouse.service.session.network.netty.SessionListener

class WebSocketBinaryHandler(private val sessionPacketListener: SessionListener
) : SimpleChannelInboundHandler<WebSocketFrame>() {
    override fun channelRead0(ctx: ChannelHandlerContext, msg: WebSocketFrame) {
        if (msg is CloseWebSocketFrame) {
            ctx.channel().writeAndFlush(msg)
                .addListener(ChannelFutureListener.CLOSE)
            return
        }
        if (msg is PingWebSocketFrame) {
            ctx.channel().writeAndFlush(PongWebSocketFrame(msg.content()))
            return
        }
        if (msg !is  BinaryWebSocketFrame) {
            throw UnsupportedOperationException("${msg.javaClass.name} frame types not supported")
        }
        ctx.fireChannelRead(msg.retain())
    }

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        ctx.pipeline().addLast(WebSocketPacketCodec()).addLast(WebSocketHandler(sessionPacketListener))
    }
}