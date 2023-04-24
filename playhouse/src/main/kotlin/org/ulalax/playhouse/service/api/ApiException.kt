package org.ulalax.playhouse.service.api

open class ApiException :Exception{
    constructor(message: String,cause:Throwable) : super(message,cause)
    constructor(message: String) : super(message)

    class DuplicatedMessageIndex: ApiException {
        constructor(message: String):super(message)
    }

    class NotRegisterApiMethod: ApiException {
        constructor(message: String):super(message)
    }
    class NotRegisterApiInstance: ApiException {
        constructor(className: String):super("$className: ApiInstance is not resister")
    }

    class NotExistApiHeaderInfoException : ApiException {
        constructor():super("target request header is not exist")
    }

}

