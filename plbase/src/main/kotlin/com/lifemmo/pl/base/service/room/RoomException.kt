package com.lifemmo.pl.base.service.room

open class RoomException : Exception {
    constructor(message: String,cause:Throwable) : super(message,cause)
    constructor(message: String) : super(message)

    class TypeIsNotExist :RoomException {
        constructor(message: String,cause:Throwable) : super(message,cause)
        constructor(message: String) : super(message)
    }


}

