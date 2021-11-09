package altchain.network.monitor.tool

import altchain.network.monitor.tool.api.ApiService
import altchain.network.monitor.tool.api.apiModule
import altchain.network.monitor.tool.persistence.persistenceModule
import altchain.network.monitor.tool.service.CleanupService
import altchain.network.monitor.tool.service.MetricsService
import altchain.network.monitor.tool.service.MonitorService
import altchain.network.monitor.tool.service.altchain.AltchainService
import altchain.network.monitor.tool.service.serviceModule
import altchain.network.monitor.tool.util.debugWarn
import org.koin.core.context.startKoin
import org.veriblock.core.utilities.Configuration
import org.veriblock.core.utilities.createLogger
import kotlin.system.exitProcess

private val logger = createLogger {}

fun main() {
    // Load config
    val configuration = Configuration()
    val koin = startKoin {
        modules(
            listOf(
                configModule(configuration),
                persistenceModule(configuration),
                serviceModule(configuration),
                apiModule(configuration)
            )
        )
    }.koin

    val apiService: ApiService = koin.get()
    val monitorService: MonitorService = koin.get()
    val altchainService: AltchainService = koin.get()
    val cleanupService: CleanupService = koin.get()
    val metricsService: MetricsService = koin.get()

    Runtime.getRuntime().addShutdownHook(Thread {
        cleanupService.stop()
        monitorService.stop()
        metricsService.stop()
        apiService.stop()
    })

    try {
        cleanupService.start()
        metricsService.start()
        altchainService.load()
        monitorService.start()
        apiService.start()
    } catch (e: Exception) {
        logger.debugWarn(e) { "There was an exception during startup" }
        logger.info { "Exiting..." }
        exitProcess(1)
    }
}