package org.ulalax.playhouse.client.network

import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.websocketx.*
import io.netty.util.CharsetUtil
import org.apache.logging.log4j.kotlin.logger


class WebSocketClientHandler(private val basePacketListener: BasePacketListener,
                             private val handshaker: WebSocketClientHandshaker) : SimpleChannelInboundHandler<Any>() {
    
    private val log = logger()
    private lateinit var handshakeFuture: ChannelPromise;
    fun handshakeFuture(): ChannelFuture {
        return handshakeFuture
    }


    override fun channelActive(ctx: ChannelHandlerContext) {
        log.debug("channelActive")
        handshaker.handshake(ctx.channel())

        ctx.fireChannelActive()
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        log.debug("WebSocket Client disconnected!")
        ctx.fireChannelInactive()
    }

    public override fun channelRead0(ctx: ChannelHandlerContext, msg: Any) {
        val ch = ctx.channel()
        if (!handshaker.isHandshakeComplete) {
            try {
                handshaker.finishHandshake(ch, msg as FullHttpResponse?)
                log.debug("WebSocket Client connected!")
                handshakeFuture.setSuccess()
            } catch (e: WebSocketHandshakeException) {
                log.debug("WebSocket Client failed to connect")
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
                log.debug("WebSocket Client received message: ${frame.text()}")
            }
            is PongWebSocketFrame -> {
                log.debug("WebSocket Client received pong")
            }
            is CloseWebSocketFrame -> {
                log.debug("WebSocket Client received closing")
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
        log.debug("handlerAdded")
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
