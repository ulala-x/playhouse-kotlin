package org.ulalax.playhouse.service.play

open class StageException : Exception {
    constructor(message: String,cause:Throwable) : super(message,cause)
    constructor(message: String) : super(message)

    class TypeIsNotExist : StageException {
        constructor(message: String,cause:Throwable) : super(message,cause)
        constructor(message: String) : super(message)
    }


}

