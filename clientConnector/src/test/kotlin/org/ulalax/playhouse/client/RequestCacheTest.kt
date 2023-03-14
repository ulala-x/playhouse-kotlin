package org.ulalax.playhouse.client

import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.ulalax.playhouse.client.network.message.ReplyPacket
import org.ulalax.playhouse.protocol.Common

internal class RequestCacheTest : AnnotationSpec(){

    @Test
    fun evictListenerExceptionTest():Unit = runBlocking  {
        val deferred = CompletableDeferred<ReplyPacket>()
        val cache = RequestCache(2)
        cache.put(1, ReplyObject(deferred = deferred))

        val result:ReplyPacket = deferred.await()
        result.errorCode.shouldBe(Common.BaseErrorCode.REQUEST_TIMEOUT_VALUE)
    }

    @Test
    fun evictListenerTest():Unit = runBlocking  {
        val deferred = CompletableDeferred<ReplyPacket>()
        val cache = RequestCache(2)

        cache.put(1, ReplyObject(deferred = deferred))
        cache.get(1).shouldNotBe(null)
    }
}