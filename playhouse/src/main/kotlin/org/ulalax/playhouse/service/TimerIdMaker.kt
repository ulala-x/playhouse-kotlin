package org.ulalax.playhouse.service

import java.util.concurrent.atomic.AtomicLong

object TimerIdMaker {
    private val timerIds = AtomicLong(0)
    fun makeId(): Long {
        return timerIds.incrementAndGet()
    }
}