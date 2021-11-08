package altchain.network.monitor.tool.util

import com.google.common.util.concurrent.ThreadFactoryBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.Job

inline fun CoroutineScope.launchWithFixedDelay(
    initialDelayMillis: Long = 0,
    periodMillis: Long = 1000L,
    crossinline block: suspend CoroutineScope.() -> Unit
): Job = launch {
    delay(initialDelayMillis)
    while (isActive) {
        block()
        delay(periodMillis)
    }
}

fun createSingleThreadExecutor(name: String): ExecutorService = Executors.newSingleThreadExecutor(
    ThreadFactoryBuilder().setNameFormat(name).build()
)

fun createMultiThreadExecutor(name: String, count: Int): ExecutorService = Executors.newFixedThreadPool(
    count,
    ThreadFactoryBuilder().setNameFormat(name).build()
)

