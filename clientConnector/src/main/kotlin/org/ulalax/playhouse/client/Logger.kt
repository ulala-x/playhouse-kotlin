package org.ulalax.playhouse.client

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

interface Logger {
    fun debug(message: String, className: String?)
    fun info(message: String, className: String?)
    fun warn(message: String, className: String?)
    fun error(message: String, className: String?, ex: Throwable? = null)
    fun trace(message: String, simpleName: String?)
    fun fatal(message: String, simpleName: String?)
}
enum class LogLevel(val intValue: Int) {
    TRACE(0),
    DEBUG(1),
    INFO(2),
    WARNING(3),
    ERROR(4),
    FATAL(5)
}
class ConsoleLogger : Logger {

    private fun getTimeStamp(): String {
        val now = LocalDateTime.now()
        return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
    }

    override fun trace(message: String, className: String?) {
        println("${getTimeStamp()} TRACE: ($className) - $message")
    }

    override fun debug(message: String, className: String?) {
        println("${getTimeStamp()} DEBUG: ($className) - $message")
    }

    override fun info(message: String, className: String?) {
        println("${getTimeStamp()} INFO: ($className) - $message")
    }

    override fun warn(message: String, className: String?) {
        println("${getTimeStamp()} WARN: ($className) - $message")
    }

    override fun error(message: String, className: String?, ex: Throwable?) {
        if (ex != null) {
            println("${getTimeStamp()} ERROR: ($className) - $message [$ex]")
        } else {
            println("${getTimeStamp()} ERROR: ($className) - $message")
        }
    }

    override fun fatal(message: String, className: String?) {
        println("${getTimeStamp()} FATAL: ($className) - $message")
    }

}

object LOG : Logger {
    private var log:Logger = ConsoleLogger()
    private var logLevel:LogLevel = LogLevel.TRACE

    fun setLogger(logger: Logger,logLevel: LogLevel = LogLevel.TRACE){
        this.log = logger
        this.logLevel = logLevel
    }

    override fun trace(message: String, className: String?) {
        if(logLevel.intValue >= LogLevel.TRACE.intValue){
            this.log.trace(message,className)
        }
    }

    override fun debug(message: String, className: String?) {
        if(logLevel.intValue >= LogLevel.DEBUG.intValue){
            this.log.debug(message,className)
        }
    }

    override fun info(message: String, className: String?) {
        if(logLevel.intValue >= LogLevel.INFO.intValue){
            this.log.info(message,className)
        }
    }

    override fun warn(message: String, className: String?) {
        if(logLevel.intValue >= LogLevel.WARNING.intValue){
            this.log.warn(message,className)
        }
    }

    override fun error(message: String, className: String?, ex: Throwable?) {
        if(logLevel.intValue >= LogLevel.ERROR.intValue){
            this.log.error(message,className)
        }
    }

    override fun fatal(message: String, className: String?) {
        if(logLevel.intValue >= LogLevel.FATAL.intValue){
            this.log.warn(message,className)
        }
    }
}
