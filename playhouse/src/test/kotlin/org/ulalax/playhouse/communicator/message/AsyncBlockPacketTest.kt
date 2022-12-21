package org.ulalax.playhouse.communicator.message

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.ulalax.playhouse.communicator.message.AsyncBlockPacket
import org.ulalax.playhouse.communicator.message.RoutePacket


internal class AsyncBlockPacketTest{


    @Test
    fun castTest(){


        val asyncBlockPacket: RoutePacket = AsyncBlockPacket.of(1, {}, "test")

        val routePacket = asyncBlockPacket as AsyncBlockPacket<Any>

        assertThat(routePacket.asyncPostCallback).isNotNull

    }
}