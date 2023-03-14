package org.ulalax.playhouse.service.session.network.netty

import org.ulalax.playhouse.communicator.IpFinder
import org.ulalax.playhouse.service.session.SessionOption
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelOption
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import org.ulalax.playhouse.Logger
import org.ulalax.playhouse.service.session.network.netty.tcp.TcpSocketServerInitializer
import org.ulalax.playhouse.service.session.network.netty.websocket.WebSocketServerInitializer

class SessionNetwork(private val sessionOption: SessionOption,
                     private val sessionPacketListener: SessionPacketListener,
                     private val log:Logger
) {

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
//            option(ChannelOption.ALLOCATOR, ByteBufferAllocator.allocator)

//            childHandler(object: ChannelInitializer<SocketChannel>() {
//                override fun initChannel(ch: SocketChannel) {
//                    val pipeline = ch.pipeline()
//                    pipeline.addLast(TcpSocketPacketCodec())
//                    pipeline.addLast(TcpSocketHandler(sessionPacketListener))
//                }
//            } )

//
//
            if(sessionOption.useWebSocket){
                childHandler(WebSocketServerInitializer(sessionOption,sessionPacketListener,log))
            }else{
                childHandler(TcpSocketServerInitializer(sessionOption,sessionPacketListener,log))

            }
        }

        channel = bootstrap.bind(port).sync().channel()

        log.info("Ready for ${IpFinder.findLocalIp()}:$port",this::class.simpleName)
    }

    fun await(){
        channel.closeFuture().sync()
    }
    fun shutdown() {
        log.info("netty shutdown",this::class.simpleName)
        NettyConfigure.shutdownGracefully()
    }
}

