package com.lifemmo.pl.base.service.api

open class ApiException :Exception{
    constructor(message: String,cause:Throwable) : super(message,cause)
    constructor(message: String) : super(message)

    class DuplicateApiHandler:ApiException {
        constructor(message: String = "Duplicate ApiHandler exist"):super(message)
    }

    class NotRegisterApiMethod:ApiException {
        constructor(msgName: String):super("$msgName: ApiMethod is not resister")
    }
    class NotRegisterApiInstance:ApiException {
        constructor(className: String):super("$className: ApiInstance is not resister")
    }

    class NotExistApiHeaderInfoException : ApiException {
        constructor():super("target request header is not exist")
    }

}

