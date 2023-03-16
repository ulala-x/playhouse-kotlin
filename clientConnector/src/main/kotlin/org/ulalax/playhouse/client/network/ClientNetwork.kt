package org.ulalax.playhouse.client.network

import org.ulalax.playhouse.client.BasePacketListener
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.DefaultHttpHeaders
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory
import io.netty.handler.codec.http.websocketx.WebSocketVersion
import kotlinx.coroutines.*
import org.ulalax.playhouse.client.network.message.ClientPacket
import org.ulalax.playhouse.client.network.tcp.TcpSocketHandler
import org.ulalax.playhouse.client.network.tcp.TcpSocketInitializer
import org.ulalax.playhouse.client.network.websocket.WebSocketClientHandler
import org.ulalax.playhouse.client.network.websocket.WebSocketInitializer
import java.net.URI

class ClientNetwork(private val connectorListener: BasePacketListener,
) {
    private lateinit var bootstrap: Bootstrap
    private val group = NioEventLoopGroup()
    private lateinit var channel: Channel
    private var websocketHandler: WebSocketClientHandler? = null
    private var tcpHandler: TcpSocketHandler? = null

    fun init(uri: URI?) {
        try {

            if(uri!=null){
                // Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
                // If you change it to V00, ping is not supported and remember to change
                // HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.
                websocketHandler = WebSocketClientHandler(
                    connectorListener,
                    WebSocketClientHandshakerFactory.newHandshaker(
                        uri, WebSocketVersion.V13, null, true, DefaultHttpHeaders()
                    )
               )
            }else{
                tcpHandler = TcpSocketHandler(connectorListener)
            }



            bootstrap = Bootstrap()
            bootstrap.group(group)
                .channel(NioSocketChannel::class.java)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,10000)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.SO_REUSEADDR, true)
                //.option(ChannelOption.ALLOCATOR, ByteBufferAllocator.allocator)

                websocketHandler?.run{
                    bootstrap.handler(WebSocketInitializer(websocketHandler!!))
                }
                tcpHandler?.run {
                    bootstrap.handler(TcpSocketInitializer(tcpHandler!!))
                }



        } catch (e: Exception) {
            group.shutdownGracefully()
            throw RuntimeException(e)
        }
    }

    suspend fun deferredConnect(host: String, port: Int, reqTimeoutSec: Long) {

        val deferred = CompletableDeferred<Channel>()

            bootstrap.connect(host, port).addListener {
            if(it.isSuccess){
                websocketHandler?.run {
                    deferred.complete(websocketHandler!!.handshakeFuture().channel())
                }

                tcpHandler?.run {
                    deferred.complete(tcpHandler!!.channel)
                }

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
        websocketHandler?.run {handshakeFuture().sync()}
    }

    fun disconnect() {
        channel.disconnect()
//        channel.writeAndFlush(CloseWebSocketFrame())
        channel.closeFuture().sync()
        group.shutdownGracefully()
    }

    fun isConnect(): Boolean {
        return channel.isActive
    }

    fun send(clientPacket: ClientPacket) {
        channel.writeAndFlush(clientPacket);
    }

}