package org.ulalax.playhouse.client.network.tcp

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel

class TcpSocketInitializer(private val handler:TcpSocketHandler,


        ) :
    ChannelInitializer<SocketChannel>() {

    override fun initChannel(socketChannel: SocketChannel) {
        val pipeline = socketChannel.pipeline()
        pipeline.addLast(
            TcpSocketPacketCodec(),
            handler
        )
    }
}