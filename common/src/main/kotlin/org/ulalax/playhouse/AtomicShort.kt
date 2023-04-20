package org.ulalax.playhouse

import java.util.concurrent.atomic.AtomicInteger

class AtomicShort {
    private val atomicInteger = AtomicInteger(0)

    fun get(): Short {
        return atomicInteger.get().toShort()
    }

    fun incrementAndGet(): Short {
        val current = atomicInteger.get()
        var next = (current + 1) and Short.MAX_VALUE.toInt()
        if(next == 0){
           next = 1
        }
        atomicInteger.compareAndSet(current, next)
        return next.toShort()
    }
}