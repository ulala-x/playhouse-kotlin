import org.apache.commons.lang3.exception.ExceptionUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

interface Logger {
    fun debug(message: String, className: String?)
    fun info(message: String, className: String?)
    fun warn(message: String, className: String?)
    fun error(message: String, className: String?, ex: Throwable? = null)
    fun trace(message: String, className: String?)
    fun fatal(message: String, className: String?)
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
        println(ExceptionUtils.getMessage(ex))
    }

    override fun fatal(message: String, className: String?) {
        println("${getTimeStamp()} FATAL: ($className) - $message")
    }

}

object LOG  {
    private var log: Logger = ConsoleLogger()
    private var logLevel: LogLevel = LogLevel.TRACE

    fun setLogger(logger: Logger, logLevel: LogLevel = LogLevel.TRACE){
        log = logger
        LOG.logLevel = logLevel
    }

    fun trace(message: String, clazz: Any) {
        if( LogLevel.TRACE.intValue >= logLevel.intValue){
            log.trace(message,clazz::class.simpleName)
        }
    }

    fun debug(message: String, clazz: Any) {
        if(LogLevel.DEBUG.intValue >= logLevel.intValue){
            log.debug(message,clazz::class.simpleName)
        }
    }

    fun info(message: String, clazz: Any) {
        if(LogLevel.INFO.intValue >= logLevel.intValue){
            log.info(message,clazz::class.simpleName)
        }
    }

    fun warn(message: String, clazz: Any) {
        if(LogLevel.WARNING.intValue >= logLevel.intValue){
            log.warn(message,clazz::class.simpleName)
        }
    }

    fun error(message: String, clazz: Any) {
        if(LogLevel.ERROR.intValue >= logLevel.intValue){
            log.error(message,clazz::class.simpleName)
        }
    }
    fun error(message: String, clazz: Any,ex: Throwable?) {
        if(LogLevel.ERROR.intValue >= logLevel.intValue){
            log.error(message,clazz::class.simpleName,ex)
        }
    }

    fun fatal(message: String, clazz: Any) {
        if(LogLevel.FATAL.intValue >= logLevel.intValue ){
            log.warn(message,clazz::class.simpleName)
        }
    }


}
