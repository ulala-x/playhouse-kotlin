package com.lifemmo.pl.base.communicator

import com.lifemmo.pl.base.communicator.message.RoutePacket
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import redis.embedded.RedisServer

object TestSession:Service{
    var onStart = false
    var onStop = false

    override fun onStart() {
        onStart = true
    }

    override fun onReceive(routePacket: RoutePacket) {
        TODO("Not yet implemented")
    }

    override fun onStop() {
        onStop = true
    }

    override fun weightPoint(): Int {
        return 10
    }

    override fun serverState(): ServerInfo.ServerState {
        return ServerInfo.ServerState.RUNNING
    }

    override fun serviceType(): ServiceType {
        return ServiceType.SESSION
    }

    override fun serviceId(): String {
        return "TestSession"
    }

    override fun pause() {
        TODO("Not yet implemented")
    }

    override fun resume() {
        TODO("Not yet implemented")
    }
}

internal class CommunicatorTest{

    companion object{
        lateinit var redisServer: RedisServer
        var port = 0


        @BeforeAll
        @JvmStatic
        fun setUp() {
            port = TestHelper.findFreePort()
            redisServer = RedisServer.builder().setting("maxmemory 128M").port(port).build()
            redisServer.start()

        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            redisServer.stop()
        }
    }

    @Test
    fun startNStop() {

    }

    @Test
    fun awaitTermination() {
    }

}