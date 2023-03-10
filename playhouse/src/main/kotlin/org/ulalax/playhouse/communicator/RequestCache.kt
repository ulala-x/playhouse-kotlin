package org.ulalax.playhouse

import com.github.benmanes.caffeine.cache.*
import org.ulalax.playhouse.protocol.Common.BaseErrorCode
import org.ulalax.playhouse.protocol.ReplyCallback
import org.ulalax.playhouse.protocol.ReplyPacket
import kotlinx.coroutines.CompletableDeferred
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.logging.log4j.kotlin.logger
import org.ulalax.playhouse.protocol.ReqPacket
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

data class ReplyObject<T: ReqPacket> (
    val callback: ReplyCallback? = null,
    val deferred: CompletableDeferred<ReplyPacket>? = null,
    val future: CompletableFuture<ReplyPacket>? = null,
){

    fun onReceive(packet: T){
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

class RequestCache<T:ReqPacket>(timeout:Long) {
    private val log = logger()
    private val sequence = AtomicInteger()
    private val cache:Cache<Int, ReplyObject<T>>

    init {
        val builder  = Caffeine.newBuilder().scheduler(Scheduler.systemScheduler())
        if(timeout > 0) {
            builder.expireAfterWrite(timeout, TimeUnit.SECONDS)
        }
        cache = builder.evictionListener(RemovalListener<Int, ReplyObject<T>> { _, replyObject, _ ->
            replyObject?.throws(BaseErrorCode.REQUEST_TIMEOUT_VALUE)
        }).build()
    }

    fun getSequence(): Int {
        return sequence.incrementAndGet()
    }

    fun put(seq: Int, replyObject: ReplyObject<T>) {
        this.cache.put(seq,replyObject)
    }
    fun get(seq:Int): ReplyObject<T>? {
        return this.cache.getIfPresent(seq)
    }

    fun onReply(packet: T) = try {
        val msgSeq = packet.header().msgSeq
        val msgName = packet.header().msgName

        cache.getIfPresent(msgSeq)?.run {
            this.onReceive(packet)
            cache.invalidate(msgSeq)
        } ?: {
            log.error("$msgSeq, $msgName request is not exist")
        }
    }catch (e:Exception){
        log.error(ExceptionUtils.getStackTrace(e))
    }
}



