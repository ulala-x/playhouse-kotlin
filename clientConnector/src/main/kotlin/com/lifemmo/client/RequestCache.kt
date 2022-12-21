package com.lifemmo.client

import com.github.benmanes.caffeine.cache.*
import com.lifemmo.pl.base.Plcommon
import com.lifemmo.pl.base.Plcommon.BaseErrorCode
import com.lifemmo.pl.base.protocol.ClientPacket
import com.lifemmo.pl.base.protocol.ReplyCallback
import com.lifemmo.pl.base.protocol.ReplyPacket
import kotlinx.coroutines.CompletableDeferred
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.logging.log4j.kotlin.logger
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

data class ReplyObject (
    val callback: ReplyCallback? = null,
    val deferred: CompletableDeferred<ReplyPacket>? = null,
    val future: CompletableFuture<ReplyPacket>? = null,
){

    fun onReceive(clientPacket: ClientPacket){
        clientPacket.use {
            callback?.onReceive(it.toReplyPacket())
            deferred?.complete(it.toReplyPacket())
            future?.complete(it.toReplyPacket())
        }
    }
    fun throws(exception:Exception){
        callback?.throws(exception)
        deferred?.completeExceptionally(exception)
        future?.completeExceptionally(exception)
    }
}

class RequestCache(timout:Long) {
    private val log = logger()
    private val sequence = AtomicInteger()
    private val cache:Cache<Int,ReplyObject>

    init {
        val builder  = Caffeine.newBuilder().scheduler(Scheduler.systemScheduler())
        if(timout > 0) {
            builder.expireAfterWrite(timout, TimeUnit.SECONDS)
        }
        cache = builder.evictionListener(RemovalListener<Int, ReplyObject> { _, replyObject, _ ->
            replyObject?.throws(ConnectorException(BaseErrorCode.REQUEST_TIMEOUT_VALUE))
        }).build()
    }

    fun getSequence(): Int {
        val seq =  sequence.incrementAndGet()
        if(seq == 0){
            return sequence.incrementAndGet()
        }
        return seq
    }

    fun put(seq: Int, replyObject: ReplyObject) {
        this.cache.put(seq,replyObject)
    }
    fun get(seq:Int): ReplyObject? {
        return this.cache.getIfPresent(seq)
    }

    fun onReply(clientPacket: ClientPacket) = try {
        val msgSeq = clientPacket.header.msgSeq

        cache.getIfPresent(msgSeq)?.run {
            val baseErrorCode = clientPacket.header.baseErrorCode
            if(baseErrorCode == BaseErrorCode.SUCCESS_VALUE){
                this.onReceive(clientPacket)
            }else{
                this.throws(ConnectorException(baseErrorCode))
            }
            cache.invalidate(msgSeq)
        } ?: {
            log.error("$msgSeq, ${clientPacket.msgName()} request is not exist")
        }
    }catch (e:Exception){
        log.error(ExceptionUtils.getStackTrace(e))
    }
}



