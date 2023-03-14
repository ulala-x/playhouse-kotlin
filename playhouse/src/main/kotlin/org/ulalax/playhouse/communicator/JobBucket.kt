package org.ulalax.playhouse.communicator

import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue


typealias Action  = ()->Unit
open class JobBucket {

    private val queue: Queue<Action> = ConcurrentLinkedQueue()

    fun add(job:Action){
        queue.add(job)
    }
    fun get(): Action? {
        return queue.poll()
    }
}

