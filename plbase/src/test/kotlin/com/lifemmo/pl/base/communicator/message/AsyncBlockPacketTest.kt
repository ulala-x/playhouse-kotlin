package com.lifemmo.pl.base.communicator.message

import com.lifemmo.pl.base.service.AsyncPostCallback
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test




internal class AsyncBlockPacketTest{


    @Test
    fun castTest(){


        val asyncBlockPacket:RoutePacket = AsyncBlockPacket.of(1, {}, "test")

        val routePacket = asyncBlockPacket as AsyncBlockPacket<Any>

        assertThat(routePacket.asyncPostCallback).isNotNull

    }
}