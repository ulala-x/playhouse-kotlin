package org.ulalax.playhouse.service

open class SenderException : Exception {
    constructor(errorCode:Int,message: String,cause:Throwable) : super(message,cause){
        this.errorCode = errorCode
    }
    constructor(errorCode:Int,message: String) : super(message){
        this.errorCode = errorCode
    }

    constructor(errorCode:Int) :super("Base ErrorCode is $errorCode"){
        this.errorCode = errorCode
    }

    var errorCode:Int = 0

}