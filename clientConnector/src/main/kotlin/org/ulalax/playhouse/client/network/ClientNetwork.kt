package org.ulalax.playhouse.client.network

import org.ulalax.playhouse.client.BasePacketListener
import org.ulalax.playhouse.base.ByteBufferAllocator
import org.ulalax.playhouse.base.protocol.ClientPacket
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.DefaultHttpHeaders
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory
import io.netty.handler.codec.http.websocketx.WebSocketVersion
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler
import kotlinx.coroutines.*
import java.net.URI

class ClientNetwork(val connectorListener: BasePacketListener) {
    private lateinit var bootstrap: Bootstrap
    private val group = NioEventLoopGroup()
    private lateinit var channel: Channel
    private lateinit var handler: WebSocketClientHandler

    fun init(uri: URI) {
        try {
            // Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
            // If you change it to V00, ping is not supported and remember to change
            // HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.
            handler = WebSocketClientHandler(
                connectorListener,
                WebSocketClientHandshakerFactory.newHandshaker(
                    uri, WebSocketVersion.V13, null, true, DefaultHttpHeaders()
                )
            )

            bootstrap = Bootstrap()
            bootstrap.group(group)
                .channel(NioSocketChannel::class.java)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,10000)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.ALLOCATOR, ByteBufferAllocator.allocator)

//                .option(ChannelOption.SO_RCVBUF, 1048576)
//                .option(ChannelOption.SO_SNDBUF, 1048576)
                .handler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(socketChannel: SocketChannel) {
                        val pipeline = socketChannel.pipeline()
                        pipeline.addLast(
                            HttpClientCodec(),
                            HttpObjectAggregator(8192),
                            WebSocketClientCompressionHandler.INSTANCE,
                            handler

                        )

                    }

                })
//            val ch: Channel = bootstrap.connect(uri.host, port).channel()
//            ch.closeFuture().sync()
        } catch (e: Exception) {
            group.shutdownGracefully()
            throw RuntimeException(e)
        }
    }

    suspend fun deferredConnect(host: String, port: Int, reqTimeoutSec: Long) {

        val deferred = CompletableDeferred<Channel>()

        bootstrap.connect(host, port).addListener {
            if(it.isSuccess){
                deferred.complete(handler.handshakeFuture().channel())
            }else{
                deferred.completeExceptionally(it.cause())
            }

        }

        withTimeout(reqTimeoutSec){
            channel = deferred.await()
        }

    }

    fun connect(host: String, port: Int){
        channel = bootstrap.connect(host, port).sync().channel()
        handler.handshakeFuture().sync()
    }

    fun disconnect() {
        channel.writeAndFlush(CloseWebSocketFrame())
        channel.closeFuture().sync()
        group.shutdownGracefully()
    }

    fun isConnect(): Boolean {
        return channel.isActive
    }

    fun send(clientPacket: ClientPacket) {
        channel.writeAndFlush(BinaryWebSocketFrame(clientPacket.toByteBuf()))
    }

}