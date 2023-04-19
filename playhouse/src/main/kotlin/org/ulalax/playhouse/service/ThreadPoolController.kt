package org.ulalax.playhouse.service

import io.netty.util.concurrent.DefaultThreadFactory
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

object ThreadPoolController {
        val workerSize = Runtime.getRuntime().availableProcessors() + 1
        //val coroutineContext = Executors.newFixedThreadPool(workerSize,DefaultThreadFactory("Coroutine",Thread.MAX_PRIORITY)).asCoroutineDispatcher()
        val coroutineAsyncCallContext = Executors.newFixedThreadPool(workerSize,DefaultThreadFactory("CoroutineAsyncCall",Thread.MAX_PRIORITY)).asCoroutineDispatcher()
}