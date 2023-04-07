package org.ulalax.playhouse

import java.util.concurrent.atomic.AtomicInteger

class AtomicShort {
    private val atomicInteger = AtomicInteger(0)

    fun get(): Short {
        return atomicInteger.get().toShort()
    }

    fun incrementAndGet(): Short {
        val current = atomicInteger.get()
        val next = (current + 1) and Short.MAX_VALUE.toInt()
        atomicInteger.compareAndSet(current, next)
        return next.toShort()
    }
}