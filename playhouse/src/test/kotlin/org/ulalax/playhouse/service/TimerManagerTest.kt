package org.ulalax.playhouse.service

import io.kotest.core.spec.style.FunSpec

import io.mockk.mockk
import io.mockk.verify
import org.ulalax.playhouse.communicator.Processor

class TimerManagerTest : FunSpec() {

    init {

        test("test registerRepeatTimer") {
            val processor: Processor = mockk(relaxed = true)
            val timerManager = TimerManager(processor)

            timerManager.registerRepeatTimer(0,1,100,100,object : TimerCallback {
                override suspend fun invoke() {
                }
            })

            Thread.sleep(350)

            verify(atLeast = 3) { processor.onReceive(any()) }
        }

        test("test registerCountTimer") {
            val processor: Processor = mockk(relaxed = true)
            val timerManager = TimerManager(processor)

            timerManager.registerCountTimer(0,2,0,3,100,object : TimerCallback {
                override suspend fun invoke() {
                }
            })

            Thread.sleep(500)
            verify(exactly = 3) { processor.onReceive(any()) }

        }

        test("test cancel timer"){
            val processor: Processor = mockk(relaxed = true)
            val timerManager = TimerManager(processor)

            val timerId = timerManager.registerCountTimer(3,1,50,3,10,object : TimerCallback {
                override suspend fun invoke() {
                }
            })

            timerManager.cancelTimer(timerId)

            Thread.sleep(300)
            verify(exactly = 0) { processor.onReceive(any()) }
        }
    }
}