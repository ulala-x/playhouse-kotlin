package com.lifemmo.client

open class ConnectorException : Exception{
    constructor(message: String,cause:Throwable) : super(message,cause)
    constructor(message: String) : super(message)

    constructor(errorCode:Int) :super("Base ErrorCode is $errorCode"){
        this.errorCode = errorCode
    }

    var errorCode:Int = 0

}

