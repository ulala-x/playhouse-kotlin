package org.ulalax.playhouse.client

import org.ulalax.playhouse.base.protocol.ReplyPacket
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class RequestCacheTest{

    @Test
    fun evictListenerExceptionTest():Unit = runBlocking  {
        val deferred = CompletableDeferred<ReplyPacket>()
        val cache = RequestCache(2)
        cache.put(1, ReplyObject(deferred = deferred))

        assertThrows<ConnectorException> {
            deferred.await()
        }
    }

    @Test
    fun evictListenerTest():Unit = runBlocking  {
        val deferred = CompletableDeferred<ReplyPacket>()
        val cache = RequestCache(2)

        cache.put(1, ReplyObject(deferred = deferred))

        assertThat(cache.get(1)).isNotNull

    }
}