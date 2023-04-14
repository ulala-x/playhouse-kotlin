package org.ulalax.playhouse.service

import java.time.Instant

class UniqueIdGenerator(private val nodeId: Int) {

    private val epoch = Instant.parse("2020-01-01T00:00:00Z").toEpochMilli()
    private var sequence = 0L
    private var lastTimestamp = -1L

    @Synchronized
    fun nextId(): Long {
        var timestamp = Instant.now().toEpochMilli() - epoch

        if (timestamp < lastTimestamp) {
            throw IllegalStateException("Clock moved backwards!")
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) and SEQUENCE_MASK
            if (sequence == 0L) {
                timestamp = nextTimestamp()
            }
        } else {
            sequence = 0L
        }

        lastTimestamp = timestamp

        return (timestamp shl TIMESTAMP_LEFT_SHIFT) or (nodeId.toLong() shl NODE_ID_SHIFT) or sequence
    }

    private fun nextTimestamp(): Long {
        var timestamp = Instant.now().toEpochMilli() - epoch

        while (timestamp <= lastTimestamp) {
            timestamp = Instant.now().toEpochMilli() - epoch
        }

        return timestamp
    }

    companion object {
        private const val NODE_ID_BITS = 12
        private const val SEQUENCE_BITS = 10

        private const val NODE_ID_SHIFT = SEQUENCE_BITS
        private const val TIMESTAMP_LEFT_SHIFT = NODE_ID_BITS + SEQUENCE_BITS

        private const val SEQUENCE_MASK = (1L shl SEQUENCE_BITS) - 1
    }
}
