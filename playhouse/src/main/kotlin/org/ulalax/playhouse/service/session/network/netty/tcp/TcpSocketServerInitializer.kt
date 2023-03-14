package org.ulalax.playhouse.service.session.network.netty.tcp

import org.ulalax.playhouse.service.session.SessionOption
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler
import io.netty.handler.timeout.IdleStateHandler
import org.ulalax.playhouse.Logger
import org.ulalax.playhouse.service.session.network.netty.SessionPacketListener
import java.util.concurrent.TimeUnit

class TcpSocketServerInitializer(private val sessionOption: SessionOption,
                                 private val sessionPacketListener: SessionPacketListener,private val log:Logger
) : ChannelInitializer<SocketChannel>() {

    override fun initChannel(ch: SocketChannel) {
        val pipeline = ch.pipeline()

        if(sessionOption.clientSessionIdleTimeout > 0){
            pipeline.addLast(IdleStateHandler(sessionOption.clientSessionIdleTimeout,
                sessionOption.clientSessionIdleTimeout,0,TimeUnit.SECONDS))
        }
        pipeline.addLast(TcpSocketPacketCodec(log))
        pipeline.addLast(TcpSocketHandler(sessionPacketListener))
    }
}