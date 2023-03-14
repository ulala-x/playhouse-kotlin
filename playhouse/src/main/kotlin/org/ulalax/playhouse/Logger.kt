package org.ulalax.playhouse

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

interface Logger {
    fun debug(message: String, className: String?)
    fun info(message: String, className: String?)
    fun warn(message: String, className: String?)
    fun error(message: String, className: String?, ex: Throwable? = null)
}

class ConsoleLogger : Logger {

    private fun getTimeStamp(): String {
        val now = LocalDateTime.now()
        return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
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
}
