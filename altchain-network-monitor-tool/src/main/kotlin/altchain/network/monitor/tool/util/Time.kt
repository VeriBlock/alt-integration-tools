package altchain.network.monitor.tool.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.ExperimentalTime

/**
 * Returns a nanosecond-truncated instant representing the current time
 */
@OptIn(ExperimentalTime::class)
fun now(): Instant {
    // Get current time
    val now = Clock.System.now()
    // Extract the nanoseconds value
    val nanosecondsOfMilli = now.nanosecondsOfSecond % 1000L
    // Subtract the nanoseconds from the retrieved time
    return now - nanoseconds(nanosecondsOfMilli)
}
