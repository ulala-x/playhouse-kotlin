package org.ulalax.playhouse.service.session.network

import org.ulalax.playhouse.base.ByteBufferAllocator
import org.ulalax.playhouse.communicator.IpFinder
import org.ulalax.playhouse.service.session.SessionOption
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelOption
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import org.apache.logging.log4j.kotlin.logger

class SessionNetwork(private val sessionOption: SessionOption,
                     private val sessionPacketListener: SessionPacketListener
) {

    private val log = logger()
    private val bootstrap = ServerBootstrap()
    private lateinit var channel:Channel
    private val backlog = 1024

    fun bind(port:Int){
        NettyConfigure.init()

        bootstrap.apply {
            group(NettyConfigure.bossGroup, NettyConfigure.workerGroup)
            channel(NettyConfigure.getChannel())
            handler(LoggingHandler(LogLevel.DEBUG))
            option(ChannelOption.SO_BACKLOG, backlog)
            childOption(ChannelOption.SO_KEEPALIVE, true)
            childOption(ChannelOption.SO_REUSEADDR, true)
            childOption(ChannelOption.SO_LINGER, 0)
            childOption(ChannelOption.TCP_NODELAY, true)
            childOption(ChannelOption.SO_RCVBUF, 64*1024)
            childOption(ChannelOption.SO_SNDBUF, 64*1024)
            //childOption(ChannelOption.SO_RCVBUF, 1024*1024)
            //childOption(ChannelOption.SO_SNDBUF, 0)
            option(ChannelOption.ALLOCATOR, ByteBufferAllocator.allocator)
            childHandler(WebSocketServerInitializer(sessionOption,sessionPacketListener))
        }



        channel = bootstrap.bind(port).sync().channel()

        log.info("Ready for ${IpFinder.findLocalIp()}:$port")
    }
    fun await(){
        channel.closeFuture().sync()
    }
    fun shutdown() {
        log.info("netty shutdown")
        NettyConfigure.shutdownGracefully()
    }
}

