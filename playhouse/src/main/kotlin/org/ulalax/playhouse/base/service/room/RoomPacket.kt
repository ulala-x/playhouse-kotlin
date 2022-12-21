package org.ulalax.playhouse.base.service.room

import com.google.protobuf.ByteString

interface RoomPacket {
    fun msgName():String
    fun message():ByteString
}