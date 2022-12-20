package com.lifemmo.pl.base.service.room

import com.google.protobuf.ByteString

class RoomPacketImpl(private val msgName:String,private val message:ByteString) : RoomPacket {
    override fun msgName(): String {
        return this.msgName
    }

    override fun message(): ByteString {
        return this.message
    }
}