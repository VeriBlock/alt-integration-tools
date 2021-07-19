package altchain.explorer.verifier.util

import mu.KLogger
import mu.KotlinLogging

fun createLogger(context: () -> Unit) = KotlinLogging.logger(context)

inline fun KLogger.debugInfo(t: Throwable, crossinline msg: () -> String) {
    info { "${msg()}: ${t.message}" }
    debug(t) { "Stack Trace:" }
}

inline fun KLogger.debugWarn(t: Throwable, crossinline msg: () -> String) {
    warn { "${msg()}: ${t.message}" }
    debug(t) { "Stack Trace:" }
}

inline fun KLogger.debugError(t: Throwable, crossinline msg: () -> String) {
    error { "${msg()}: ${t.message}" }
    debug(t) { "Stack Trace:" }
}
