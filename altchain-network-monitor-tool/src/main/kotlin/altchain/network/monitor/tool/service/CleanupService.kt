package altchain.network.monitor.tool.service

import altchain.network.monitor.tool.CleanupConfig
import altchain.network.monitor.tool.persistence.repositories.AbfiMonitorRepository
import altchain.network.monitor.tool.persistence.repositories.AltDaemonMonitorRepository
import altchain.network.monitor.tool.persistence.repositories.ExplorerMonitorRepository
import altchain.network.monitor.tool.persistence.repositories.MinerMonitorRepository
import altchain.network.monitor.tool.persistence.repositories.NodeCoreMonitorRepository
import altchain.network.monitor.tool.persistence.repositories.PopSubsidiesRepository
import altchain.network.monitor.tool.persistence.repositories.VbfiMonitorRepository
import altchain.network.monitor.tool.util.createLogger
import altchain.network.monitor.tool.util.createSingleThreadExecutor
import altchain.network.monitor.tool.util.launchWithFixedDelay
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel

private val logger = createLogger {}

class CleanupService(
    private val cleanupConfig: CleanupConfig,
    private val abfiMonitorRepository: AbfiMonitorRepository,
    private val vbfiMonitorRepository: VbfiMonitorRepository,
    private val altDaemonMonitorRepository: AltDaemonMonitorRepository,
    private val explorerMonitorRepository: ExplorerMonitorRepository,
    private val minerMonitorRepository: MinerMonitorRepository,
    private val nodeCoreMonitorRepository: NodeCoreMonitorRepository,
    private val popSubsidiesRepository: PopSubsidiesRepository
) {
    private val coroutineScope by lazy {
        CoroutineScope(createSingleThreadExecutor("cleanup-service-thread").asCoroutineDispatcher())
    }

    fun start() {
        if (cleanupConfig.runDelay > 0) {
            coroutineScope.launchWithFixedDelay(
                initialDelayMillis = TimeUnit.SECONDS.toMillis(1),
                periodMillis = TimeUnit.HOURS.toMillis(cleanupConfig.runDelay)
            ) {
                logger.info { "Starting the cleanup task..." }
                val rowsDeleted = abfiMonitorRepository.deleteOldData(cleanupConfig.hoursAgo) +
                altDaemonMonitorRepository.deleteOldData(cleanupConfig.hoursAgo) +
                explorerMonitorRepository.deleteOldData(cleanupConfig.hoursAgo) +
                minerMonitorRepository.deleteOldData(cleanupConfig.hoursAgo) +
                nodeCoreMonitorRepository.deleteOldData(cleanupConfig.hoursAgo) +
                popSubsidiesRepository.deleteOldData(cleanupConfig.hoursAgo) +
                vbfiMonitorRepository.deleteOldData(cleanupConfig.hoursAgo)
                logger.info { "Finished the cleanup task, $rowsDeleted rows have been deleted" }
            }
        }
    }

    fun stop() {
        if (cleanupConfig.runDelay > 0) {
            coroutineScope.cancel()
        }
    }
}