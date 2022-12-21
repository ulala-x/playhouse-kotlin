package org.ulalax.playhouse.base.service.room.base

import java.util.concurrent.atomic.AtomicLong

object TimerIdMaker {
    private val timerIds = AtomicLong(0)
    fun makeId(): Long {
        return timerIds.incrementAndGet()
    }
}