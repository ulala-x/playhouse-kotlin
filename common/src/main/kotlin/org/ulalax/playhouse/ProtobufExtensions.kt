package org.ulalax.playhouse

import com.google.protobuf.Message
import com.google.protobuf.MessageLite
import com.google.protobuf.Parser

object ProtobufExtensions {
    fun <T : MessageLite> Parser<T>.parseFrom(inputs: Pair<ByteArray, Int>): T {
        val (buffer, length) = inputs
        return this.parseFrom(buffer, 0, length)
    }

    fun <T : Message> Parser<T>.parseFrom(inputs: Pair<ByteArray, Int>): T {
        val (buffer, length) = inputs
        return this.parseFrom(buffer, 0, length)
    }
}