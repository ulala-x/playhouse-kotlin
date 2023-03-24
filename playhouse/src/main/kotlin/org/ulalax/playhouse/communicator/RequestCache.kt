package org.ulalax.playhouse.communicator

import com.github.benmanes.caffeine.cache.*
import org.ulalax.playhouse.protocol.Common.BaseErrorCode
import kotlinx.coroutines.CompletableDeferred
import org.apache.commons.lang3.exception.ExceptionUtils
import LOG
import org.ulalax.playhouse.communicator.message.ReplyCallback
import org.ulalax.playhouse.communicator.message.ReplyPacket
import org.ulalax.playhouse.communicator.message.RoutePacket
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

data class ReplyObject (
        val callback: ReplyCallback? = null,
        val deferred: CompletableDeferred<ReplyPacket>? = null,
        val future: CompletableFuture<ReplyPacket>? = null,
){

    fun onReceive(packet: RoutePacket){
        callback?.onReceive(packet.toReplyPacket())
        deferred?.complete(packet.toReplyPacket())
        future?.complete(packet.toReplyPacket())
    }
    fun throws(errorCode: Int){
        callback?.onReceive(ReplyPacket(errorCode))
        deferred?.complete(ReplyPacket(errorCode))
        future?.complete(ReplyPacket(errorCode))
    }
}

class RequestCache(timeout:Long) {

    private val sequence = AtomicInteger()
    private val cache:Cache<Int, ReplyObject>

    init {
        val builder  = Caffeine.newBuilder().scheduler(Scheduler.systemScheduler())
        if(timeout > 0) {
            builder.expireAfterWrite(timeout, TimeUnit.SECONDS)
        }
        cache = builder.evictionListener(RemovalListener<Int, ReplyObject> { _, replyObject, _ ->
            replyObject?.throws(BaseErrorCode.REQUEST_TIMEOUT_VALUE)
        }).build()
    }

    fun getSequence(): Int {
        return sequence.incrementAndGet()
    }

    fun put(seq: Int, replyObject: ReplyObject) {
        this.cache.put(seq,replyObject)
    }
    fun get(seq:Int): ReplyObject? {
        return this.cache.getIfPresent(seq)
    }

    fun onReply(packet: RoutePacket) = try {
        val msgSeq = packet.header().msgSeq
        val msgName = packet.header().msgName

        cache.getIfPresent(msgSeq)?.run {
            this.onReceive(packet)
            cache.invalidate(msgSeq)
        } ?: {
            LOG.error("$msgSeq, $msgName request is not exist",this)
        }
    }catch (e:Exception){
        LOG.error(ExceptionUtils.getStackTrace(e),this,e)
    }
}



