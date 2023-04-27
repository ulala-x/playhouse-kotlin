package org.ulalax.playhouse.client

import com.github.benmanes.caffeine.cache.*
import kotlinx.coroutines.CompletableDeferred
import org.apache.commons.lang3.exception.ExceptionUtils
import org.ulalax.playhouse.AtomicShort
import org.ulalax.playhouse.client.network.message.ClientPacket
import org.ulalax.playhouse.client.network.message.ReplyCallback
import org.ulalax.playhouse.client.network.message.ReplyPacket
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import org.ulalax.playhouse.protocol.Common.*

data class ReplyObject (
        val callback: ReplyCallback? = null,
        val deferred: CompletableDeferred<ReplyPacket>? = null,
        val future: CompletableFuture<ReplyPacket>? = null,
){

    fun onReceive(packet: ClientPacket){
        callback?.onReceive(packet.toReplyPacket())
        deferred?.complete(packet.toReplyPacket())
        future?.complete(packet.toReplyPacket())
    }
    fun throws(errorCode: Short){
        callback?.onReceive(ReplyPacket(errorCode,0))
        deferred?.complete(ReplyPacket(errorCode,0))
        future?.complete(ReplyPacket(errorCode,0))
    }
}

class RequestCache(timeout:Long){

    private val sequence = AtomicShort()
    private val cache:Cache<Short, ReplyObject>

    init {
        val builder  = Caffeine.newBuilder().scheduler(Scheduler.systemScheduler())
        if(timeout > 0) {
            builder.expireAfterWrite(timeout, TimeUnit.SECONDS)
        }
        cache = builder.evictionListener(RemovalListener<Short, ReplyObject> { _, replyObject, _ ->
            replyObject?.throws(BaseErrorCode.REQUEST_TIMEOUT_VALUE.toShort())
        }).build()
    }

    fun getSequence(): Short {
        return sequence.incrementAndGet()
    }

    fun put(seq: Short, replyObject: ReplyObject) {
        this.cache.put(seq,replyObject)
    }
    fun get(seq:Short): ReplyObject? {
        return this.cache.getIfPresent(seq)
    }

    fun onReply(packet: ClientPacket) = try {
        val msgSeq = packet.header().msgSeq
        val msgId = packet.header().msgId

        cache.getIfPresent(msgSeq)?.run {
            this.onReceive(packet)
            cache.invalidate(msgSeq)
        } ?: {
            LOG.error("$msgSeq, $msgId request is not exist",this)
        }
    }catch (e:Exception){
        LOG.error(ExceptionUtils.getStackTrace(e),this,e)
    }
}



