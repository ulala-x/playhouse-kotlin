package org.ulalax.playhouse.base.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalListener
import com.github.benmanes.caffeine.cache.Scheduler
import org.ulalax.playhouse.base.ErrorCode
import org.ulalax.playhouse.base.communicator.message.RoutePacket
import org.ulalax.playhouse.base.protocol.ReplyCallback
import org.ulalax.playhouse.base.protocol.ReplyPacket
import kotlinx.coroutines.CompletableDeferred
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.logging.log4j.kotlin.logger
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

data class ReplyObject (
    val callback: ReplyCallback? = null,
    val deferred: CompletableDeferred<ReplyPacket>? = null,
    val future: CompletableFuture<ReplyPacket>? = null,
){

    fun onReceive(routePacket: RoutePacket){
        callback?.onReceive(routePacket.toReplyPacket())
        deferred?.complete(routePacket.toReplyPacket())
        future?.complete(routePacket.toReplyPacket())
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

    private val cache: Cache<Int, ReplyObject>

    init{
        val builder  = Caffeine.newBuilder().scheduler(Scheduler.systemScheduler())
        if(timout > 0) {
            builder.expireAfterWrite(timout, TimeUnit.SECONDS)
        }

        builder.evictionListener(RemovalListener<Int, ReplyObject> {
                _, replyObject, _ -> replyObject?.throws(SenderException(ErrorCode.REQUEST_TIMEOUT))
        })
        cache = builder.build()
    }


    fun getSequence(): Int {
        val seq = sequence.incrementAndGet()
        if (seq == 0){
            return sequence.incrementAndGet()
        }
        return seq
    }

    fun put(seq: Int, replyObject: ReplyObject) {
        this.cache.put(seq,replyObject)
    }


    fun onReply(routePacket: RoutePacket) = try {
        val header = routePacket.routeHeader.header
        val replyObject = cache.getIfPresent(header.msgSeq)
        if(replyObject!=null){
            val baseErrorCode = header.baseErrorCode
            if(baseErrorCode != ErrorCode.SUCCESS){
                replyObject.throws(SenderException(baseErrorCode))
            }else{
                replyObject.onReceive(routePacket)
            }
        }else{
            log.error{"${header.msgName}, ${header.msgSeq} is not reply packet or timeout"}
        }
        cache.invalidate(header.msgSeq)
    }catch (e:Exception){
        log.error(ExceptionUtils.getStackTrace(e))
    }
}
