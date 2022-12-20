package com.lifemmo.pl.base.service.room.contents

import com.lifemmo.pl.base.protocol.Packet


interface PacketCmd<R,U> {
    suspend fun execute(room: R, user: U, packet: Packet)
}