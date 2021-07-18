package altchain.explorer.verifier.service

import altchain.explorer.verifier.ExplorerConfig
import altchain.explorer.verifier.persistence.ExplorerState
import altchain.explorer.verifier.persistence.ExplorerStateRepository
import altchain.explorer.verifier.util.Configuration
import altchain.explorer.verifier.util.createLogger
import altchain.explorer.verifier.util.createMultiThreadExecutor
import altchain.explorer.verifier.util.createSingleThreadExecutor
import altchain.explorer.verifier.util.debugWarn
import altchain.explorer.verifier.util.launchWithFixedDelay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.util.concurrent.TimeUnit

private val logger = createLogger {}

class ExplorerStatusService(
    configuration: Configuration,
    private val explorerStateRepository: ExplorerStateRepository,
    private val explorersConfig: Map<String, ExplorerConfig>,
    private val explorerService: ExplorerService
) {
    private val executor = createSingleThreadExecutor("explorer-status-thread")
    private val coroutineScope = CoroutineScope(executor.asCoroutineDispatcher())

    private val explorerParserExecutor = createMultiThreadExecutor("explorer-parser-thread", 16)
    private val explorerParserCoroutineScope = CoroutineScope(explorerParserExecutor.asCoroutineDispatcher())

    private val checkDelay = configuration.getLong("checkDelay") ?: 10

    fun start() {
        coroutineScope.launchWithFixedDelay(
            initialDelayMillis = TimeUnit.SECONDS.toMillis(1),
            periodMillis = TimeUnit.MINUTES.toMillis(checkDelay)
        ) {
            explorersConfig.entries.forEach { (key, value) ->
                try {
                    explorerParserCoroutineScope.launch {
                        logger.info { "($key) Checking ${value.url} with blockCount @ ${value.blockCount}" }
                        val blockInfo = explorerService.getExplorerState(value)
                        val atvBlocks = blockInfo.filter {
                            it.atvs > 0
                        }.asSequence().mapNotNull {
                            it.height
                        }.toSet()

                        val vtbBlocks = blockInfo.filter {
                            it.vtbs > 0
                        }.asSequence().mapNotNull {
                            it.height
                        }.toSet()

                        val vbkBlocks = blockInfo.filter {
                            it.vbks > 0
                        }.asSequence().mapNotNull {
                            it.height
                        }.toSet()

                        val explorerState = ExplorerState(
                            configName = key,
                            url = value.url,
                            blockCount = value.blockCount,
                            atvBlocks = atvBlocks,
                            vtbBlocks = vtbBlocks,
                            vbkBlocks = vbkBlocks,
                            atvCount = atvBlocks.size,
                            vtbCount = vtbBlocks.size,
                            vbkCount = vbkBlocks.size,
                            addedAt = Clock.System.now()
                        )
                        explorerStateRepository.addExplorerState(explorerState)
                        logger.info { "($key) Added new explorer state" }
                    }
                } catch (exception: Exception) {
                    logger.debugWarn(exception) { "($key) Failed to parse the explorer url ${value.url}" }
                }
            }
        }
    }
    
    fun stop() {
        coroutineScope.cancel()
        explorerParserCoroutineScope.cancel()
    }
}