package org.ulalax.playhouse.communicator

open class CommunicatorException : Exception {
    constructor(message: String,cause:Throwable) : super(message,cause)
    constructor(message: String) : super(message)

    class NotExistServerInfo(errMsg: String) : CommunicatorException(errMsg) {
    }

}

