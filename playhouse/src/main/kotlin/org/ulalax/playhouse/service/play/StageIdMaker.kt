package org.ulalax.playhouse.service.play

import java.util.concurrent.atomic.AtomicLong

object StageIdMaker {
    private val groupIds = AtomicLong()
    //10초 동안 10000개의 unique 한 id 값을 생성
    fun makeId(): Long {
        return System.currentTimeMillis()/10000*10000 + groupIds.incrementAndGet()%10000
    }
}