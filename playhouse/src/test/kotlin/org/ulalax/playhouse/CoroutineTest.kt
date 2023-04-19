package org.ulalax.playhouse

import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.*
import java.util.*

class CoroutineTest : FunSpec(){

    init {

        test("coroutine test"){
            val queue: Queue<suspend  () -> Unit> = LinkedList()
            val queue1: Queue<suspend  () -> Unit> = LinkedList()

            // Queue에 3개의 작업을 추가합니다.
            queue.add { delay(100); println("Job 1") }
            queue.add { delay(50); println("Job 2") }
            queue.add { delay(10); println("Job 3") }

            queue1.add { delay(100); println("Job 1") }
            queue1.add { delay(50); println("Job 2") }
            queue1.add { delay(10); println("Job 3") }

            val dispatcher = newSingleThreadContext("myThread")
            val startTime = System.currentTimeMillis()

            val jobs = mutableListOf<Deferred<Unit>>()

            jobs.add(async(dispatcher) {
                while (queue.isNotEmpty()) {
                    val job = queue.poll()
                    //val deferred = async {
                        job()
                    //}
                    println("작업을 시작합니다.") // 스레드가 다른 작업을 수행합니다.
                    //deferred.await() // 현재 코루틴이 block됩니다.
                    println("작업이 완료되었습니다.") // 스레드가 다른 작업을 수행합니다.
                }
                println("모든 작업이 완료되었습니다.")
            })

            jobs.add(async(dispatcher) {
                while (queue.isNotEmpty()) {
                    val job = queue1.poll()
                    //val deferred = async {
                    job()
                    //}
                    println("작업을 시작합니다.") // 스레드가 다른 작업을 수행합니다.
                    //deferred.await() // 현재 코루틴이 block됩니다.
                    println("작업이 완료되었습니다.") // 스레드가 다른 작업을 수행합니다.
                }
                println("모든 작업이 완료되었습니다.")
            })

            jobs.forEach { it.await() }

            val endTime = System.currentTimeMillis()
            val elapsedTime = endTime - startTime
            println("Elapsed time: $elapsedTime ms")
        }

    }
}