package org.ulalax.playhouse.client.network.websocket

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler

class WebSocketInitializer(private val handler:WebSocketClientHandler) :
    ChannelInitializer<SocketChannel>() {

    override fun initChannel(socketChannel: SocketChannel) {
        val pipeline = socketChannel.pipeline()
        pipeline.addLast(
            HttpClientCodec(),
            HttpObjectAggregator(8192),
            WebSocketClientCompressionHandler.INSTANCE,
            handler
        )
    }
}