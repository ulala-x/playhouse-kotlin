package org.ulalax.playhouse

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

class NamedThreadFactory(name: String) : ThreadFactory {
    private val threadNumber = AtomicInteger(1)
    private val namePrefix: String

    init {
        namePrefix = "$name-thread-"
    }

    override fun newThread(runnable: Runnable): Thread {
        return Thread(runnable, namePrefix + threadNumber.getAndIncrement())
    }
}