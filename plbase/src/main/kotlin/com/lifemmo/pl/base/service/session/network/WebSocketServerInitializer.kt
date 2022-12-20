package com.lifemmo.pl.base.service.session.network

import com.lifemmo.pl.base.service.session.SessionOption
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler
import io.netty.handler.timeout.IdleStateHandler
import java.util.concurrent.TimeUnit

class WebSocketServerInitializer(private val sessionOption: SessionOption,
                                 private val sessionPacketListener: SessionPacketListener) : ChannelInitializer<SocketChannel>() {
    private val webSocketPath = "/websocket"

    override fun initChannel(ch: SocketChannel) {
        val pipeline = ch.pipeline()
        pipeline.addLast(HttpServerCodec())
        pipeline.addLast(HttpObjectAggregator(65536))
        pipeline.addLast(WebSocketServerCompressionHandler())
        pipeline.addLast(WebSocketServerProtocolHandler(webSocketPath, null, true))
        if(sessionOption.clientSessionIdleTimeout > 0){
            pipeline.addLast(IdleStateHandler(sessionOption.clientSessionIdleTimeout,
                sessionOption.clientSessionIdleTimeout,0,TimeUnit.SECONDS))
        }
        pipeline.addLast(WebSocketBinaryHandler(sessionPacketListener))
    }
}