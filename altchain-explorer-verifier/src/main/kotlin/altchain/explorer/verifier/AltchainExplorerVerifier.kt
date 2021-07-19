package altchain.explorer.verifier

import altchain.explorer.verifier.api.ApiService
import altchain.explorer.verifier.api.apiModule
import altchain.explorer.verifier.persistence.persistenceModule
import altchain.explorer.verifier.service.ExplorerStatusService
import altchain.explorer.verifier.service.serviceModule
import altchain.explorer.verifier.util.Configuration
import altchain.explorer.verifier.util.createLogger
import altchain.explorer.verifier.util.debugWarn
import org.koin.core.context.startKoin
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
    val explorerStatusService: ExplorerStatusService = koin.get()

    Runtime.getRuntime().addShutdownHook(Thread {
        explorerStatusService.stop()
        apiService.stop()
    })

    try {
        explorerStatusService.start()
        apiService.start()
    } catch (e: Exception) {
        logger.debugWarn(e) { "There was an exception during startup" }
        logger.info { "Exiting..." }
        exitProcess(1)
    }
}