package org.ulalax.playhouse.base.communicator.message

import org.ulalax.playhouse.base.service.AsyncPostCallback
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