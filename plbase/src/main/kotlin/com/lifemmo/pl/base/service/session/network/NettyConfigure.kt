package com.lifemmo.pl.base.service.session.network

import io.netty.channel.EventLoopGroup
import io.netty.channel.ServerChannel
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.util.concurrent.DefaultThreadFactory
import org.apache.logging.log4j.kotlin.logger

object NettyConfigure {
    val log = logger()

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
            log.info("============Start with Epoll $avaiable============")
            bossGroup = EpollEventLoopGroup(1, DefaultThreadFactory("lbase-epoll-boss", true))
            workerGroup = EpollEventLoopGroup(2, DefaultThreadFactory("lbase-epoll-worker", true))
        } else {
            log.info("=============Start with Nio $avaiable=============")
            bossGroup = NioEventLoopGroup(1, DefaultThreadFactory("lbase-nio-boss", true))
            workerGroup = NioEventLoopGroup(1, DefaultThreadFactory("lbase-nio-worker", true))
        }
    }

}