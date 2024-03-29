package org.ulalax.playhouse.service.session.network.netty

import io.netty.channel.EventLoopGroup
import io.netty.channel.ServerChannel
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.util.concurrent.DefaultThreadFactory
import LOG

object NettyConfigure {

    lateinit var bossGroup: EventLoopGroup
    lateinit var workerGroup: EventLoopGroup


    fun getChannel(): Class<out ServerChannel> {
        return if (Epoll.isAvailable()) {
            EpollServerSocketChannel::class.java
        } else {
            NioServerSocketChannel::class.java
        }
    }

    fun shutdownGracefully() {
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
    }

    fun init() {
        var avaiable = Runtime.getRuntime().availableProcessors()
        if (Epoll.isAvailable()) {
            LOG.info("============Start with Epoll $avaiable============",this)
            bossGroup = EpollEventLoopGroup(1, DefaultThreadFactory("session:epoll-boss", true))
            workerGroup = EpollEventLoopGroup(1, DefaultThreadFactory("session:epoll-worker", true))
        } else {
            LOG.info("=============Start with Nio $avaiable=============",this)
            bossGroup = NioEventLoopGroup(1, DefaultThreadFactory("session:nio-boss", true))
            workerGroup = NioEventLoopGroup(1, DefaultThreadFactory("session:nio-worker", true))
        }
    }

}