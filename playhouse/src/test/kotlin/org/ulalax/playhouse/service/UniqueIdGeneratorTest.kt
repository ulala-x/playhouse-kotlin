package org.ulalax.playhouse.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class UniqueIdGeneratorTest : FunSpec({

    val generator = UniqueIdGenerator(1)

    test("generated IDs should be unique") {
        val ids = mutableSetOf<Long>()
        for (i in 1..10000) {
            val id = generator.nextId()
            ids.add(id)
        }
        ids.size shouldBe 10000
    }

    test("generated IDs should be in order") {
        val ids = mutableListOf<Long>()
        for (i in 1..100) {
            val id = generator.nextId()
            ids.add(id)
        }
        ids shouldBe ids.sorted()
    }

    test("generated IDs should be thread-safe") {
        val pool = Executors.newFixedThreadPool(10)
        val ids = mutableSetOf<Long>()
        for (i in 1..10000) {
            pool.submit {
                val id = generator.nextId()
                synchronized(ids) {
                    ids.add(id)
                }
            }
        }
        pool.shutdown()
        pool.awaitTermination(1, TimeUnit.MINUTES)
        ids.size shouldBe 10000
    }
})




