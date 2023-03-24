package org.ulalax.playhouse.client.network.websocket

import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.websocketx.*
import io.netty.util.CharsetUtil
import org.ulalax.playhouse.client.network.BasePacketListener


class WebSocketClientHandler(private val basePacketListener: BasePacketListener,
                             private val handshaker: WebSocketClientHandshaker,
) : SimpleChannelInboundHandler<Any>() {
    

    private lateinit var handshakeFuture: ChannelPromise;
    fun handshakeFuture(): ChannelFuture {
        return handshakeFuture
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        LOG.debug("channelActive",this)
        handshaker.handshake(ctx.channel())

        ctx.fireChannelActive()
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        LOG.debug("WebSocket Client disconnected!",this)
        ctx.fireChannelInactive()
    }

    public override fun channelRead0(ctx: ChannelHandlerContext, msg: Any) {
        val ch = ctx.channel()
        if (!handshaker.isHandshakeComplete) {
            try {
                handshaker.finishHandshake(ch, msg as FullHttpResponse?)
                LOG.debug("WebSocket Client connected!",this)
                handshakeFuture.setSuccess()
            } catch (e: WebSocketHandshakeException) {
                LOG.debug("WebSocket Client failed to connect",this)
                handshakeFuture.setFailure(e)
            }
            return
        }
        if (msg is FullHttpResponse) {
            throw IllegalStateException(
                "Unexpected FullHttpResponse (getStatus=" + msg.status() +
                        ", content=" + msg.content().toString(CharsetUtil.UTF_8) + ')'
            )
        }
        when (val frame = msg as WebSocketFrame) {
            is TextWebSocketFrame -> {
                LOG.debug("WebSocket Client received message: ${frame.text()}",this)
            }
            is PongWebSocketFrame -> {
                LOG.debug("WebSocket Client received pong",this)
            }
            is CloseWebSocketFrame -> {
                LOG.debug("WebSocket Client received closing",this)
                ch.close()
                return
            }
        }
        if (msg !is  BinaryWebSocketFrame) {
            throw UnsupportedOperationException("${msg.javaClass.name} frame types not supported")
        }
        ctx.fireChannelRead(msg.retain())
    }

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        LOG.debug("handlerAdded",this)
        ctx.pipeline().addLast(WebSocketPacketCodec()).addLast(WebSocketHandler(basePacketListener))
        handshakeFuture = ctx.newPromise()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        if (!handshakeFuture.isDone) {
            handshakeFuture.setFailure(cause)
        }
        ctx.close()
    }
}
