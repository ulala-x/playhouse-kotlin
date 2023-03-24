package org.ulalax.playhouse.service.session.network.netty.tcp

import org.ulalax.playhouse.service.session.SessionOption
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.timeout.IdleStateHandler
import org.ulalax.playhouse.service.session.network.netty.SessionListener
import java.util.concurrent.TimeUnit

class TcpSocketServerInitializer(private val sessionOption: SessionOption,
                                 private val sessionPacketListener: SessionListener)
    : ChannelInitializer<SocketChannel>() {

    override fun initChannel(ch: SocketChannel) {
        val pipeline = ch.pipeline()

        if(sessionOption.clientSessionIdleTimeout > 0){
            pipeline.addLast(IdleStateHandler(sessionOption.clientSessionIdleTimeout,
                sessionOption.clientSessionIdleTimeout,0,TimeUnit.SECONDS))
        }
        pipeline.addLast(TcpSocketPacketCodec())
        pipeline.addLast(TcpSocketHandler(sessionPacketListener))
    }
}