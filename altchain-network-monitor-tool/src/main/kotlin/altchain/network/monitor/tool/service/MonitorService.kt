package altchain.network.monitor.tool.service

import altchain.network.monitor.tool.NetworkConfig
import altchain.network.monitor.tool.persistence.repositories.AbfiMonitorRepository
import altchain.network.monitor.tool.persistence.repositories.AltDaemonMonitorRepository
import altchain.network.monitor.tool.persistence.repositories.ExplorerMonitorRepository
import altchain.network.monitor.tool.persistence.repositories.MinerMonitorRepository
import altchain.network.monitor.tool.persistence.repositories.NodeCoreMonitorRepository
import altchain.network.monitor.tool.service.abfi.AbfiService
import altchain.network.monitor.tool.service.altchain.AltchainService
import altchain.network.monitor.tool.service.nodecore.NodeCoreService
import altchain.network.monitor.tool.util.createLogger
import altchain.network.monitor.tool.util.createMultiThreadExecutor
import altchain.network.monitor.tool.util.debugWarn
import altchain.network.monitor.tool.util.launchWithFixedDelay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch

private val logger = createLogger {}

class MonitorService(
    private val nodeCoreService: NodeCoreService,
    private val abfiService: AbfiService,

    private val minerMonitorRepository: MinerMonitorRepository,
    private val explorerStateRepository: ExplorerMonitorRepository,
    private val nodeCoreMonitorRepository: NodeCoreMonitorRepository,
    private val abfiMonitorRepository: AbfiMonitorRepository,
    private val altDaemonMonitorRepository: AltDaemonMonitorRepository,
    private val altchainService: AltchainService,
    private val minerService: MinerService,
    private val explorerService: ExplorerService,
    private val netwrokConfigs: Map<String, NetworkConfig>
) {
    private val monitorServiceExecutor = createMultiThreadExecutor("monitor-service-thread", 16)
    private val monitorServiceCoroutineScope = CoroutineScope(monitorServiceExecutor.asCoroutineDispatcher())

    private val minerMonitorExecutor = createMultiThreadExecutor("miner-monitor-thread", 8)
    private val minerMonitorCoroutineScope = CoroutineScope(minerMonitorExecutor.asCoroutineDispatcher())

    private val nodecoreMonitorExecutor = createMultiThreadExecutor("nodecore-monitor-thread", 8)
    private val nodecoreMonitorCoroutineScope = CoroutineScope(nodecoreMonitorExecutor.asCoroutineDispatcher())

    private val altDaemonMonitorExecutor = createMultiThreadExecutor("alt-daemon-monitor-thread", 8)
    private val altDaemonMonitorCoroutineScope = CoroutineScope(altDaemonMonitorExecutor.asCoroutineDispatcher())

    private val abfirMonitorExecutor = createMultiThreadExecutor("abfi-monitor-thread", 8)
    private val abfiMonitorCoroutineScope = CoroutineScope(abfirMonitorExecutor.asCoroutineDispatcher())

    private val explorerMonitorExecutor = createMultiThreadExecutor("explorer-monitor-thread", 8)
    private val explorerMonitorCoroutineScope = CoroutineScope(explorerMonitorExecutor.asCoroutineDispatcher())

    fun start() {
        netwrokConfigs.entries.forEach { (networkKey, networkConfig) ->
            monitorServiceCoroutineScope.launchWithFixedDelay(
                initialDelayMillis = TimeUnit.SECONDS.toMillis(1),
                periodMillis = TimeUnit.MINUTES.toMillis(networkConfig.checkDelay)
            ) {
                try {
                    // Miners
                    networkConfig.miners.forEach { (minerKey, minerConfig) ->
                        minerMonitorCoroutineScope.launch {
                            try {
                                logger.info { "($networkKey/$minerKey) Monitoring..." }
                                val record = minerService.getMinerMonitor(
                                    networkId = networkKey,
                                    minerId = minerKey,
                                    minerConfig = minerConfig
                                )
                                minerMonitorRepository.create(
                                    networkId = networkKey,
                                    minerId = minerKey,
                                    host = minerConfig.apiUrl,
                                    minerType = minerConfig.type,
                                    monitor = record
                                )
                                logger.info { "($networkKey/$minerKey) Added a new state" }
                            } catch (exception: Exception) {
                                logger.debugWarn(exception) { "($networkKey/$minerKey) Failed to monitor" }
                            }
                        }
                    }

                    // NodeCores
                    networkConfig.nodecores.forEach { (nodecoreKey, nodecoreConfig) ->
                        nodecoreMonitorCoroutineScope.launch {
                            try {
                                logger.info { "($networkKey/$nodecoreKey) Monitoring..." }
                                val record = nodeCoreService.getNodeCoreMonitor(
                                    networkId = networkKey,
                                    nodecoreId = nodecoreKey,
                                    nodeCoreConfig = nodecoreConfig
                                )
                                nodeCoreMonitorRepository.create(
                                    networkId = networkKey,
                                    nodecoreId = nodecoreKey,
                                    host = nodecoreConfig.host,
                                    monitor = record
                                )
                                logger.info { "($networkKey/$nodecoreKey) Added a new state" }
                            } catch (exception: Exception) {
                                logger.debugWarn(exception) { "($networkKey/$nodecoreKey) Failed to monitor" }
                            }
                        }
                    }

                    // Alt Daemons
                    networkConfig.altDaemons.forEach { (altDaemonKey, altDaemonConfig) ->
                        altDaemonMonitorCoroutineScope.launch {
                            try {
                                logger.info { "($networkKey/$altDaemonKey) Monitoring..." }
                                val record = altchainService.getBlockChainInfo(altDaemonConfig.siKey)
                                altDaemonMonitorRepository.create(
                                    networkId = networkKey,
                                    altDaemonId = altDaemonKey,
                                    host = record.host,
                                    monitor = record
                                )
                                logger.info { "($networkKey/$altDaemonKey) Added a new state" }
                            } catch (exception: Exception) {
                                logger.debugWarn(exception) { "($networkKey/$altDaemonKey) Failed to monitor" }
                            }
                        }
                    }

                    // ABFIs
                    networkConfig.abfis.forEach { (abfiKey, abfiConfig) ->
                        abfiMonitorCoroutineScope.launch {
                            try {
                                logger.info { "($networkKey/$abfiKey) Monitoring..." }
                                val record = abfiService.getAbfiMonitor(
                                    networkId = networkKey,
                                    abfiId = abfiKey,
                                    abfiConfig = abfiConfig
                                )
                                abfiMonitorRepository.create(
                                    networkId = networkKey,
                                    abfiId = abfiKey,
                                    host = abfiConfig.apiUrl,
                                    prefix = abfiConfig.prefix,
                                    monitor = record
                                )
                                logger.info { "($networkKey/$abfiKey) Added new abfi state" }
                            } catch (exception: Exception) {
                                logger.debugWarn(exception) { "($networkKey/$abfiKey) Failed to monitor" }
                            }
                        }
                    }

                    // Explorers
                    networkConfig.explorers.forEach { (explorerKey, explorerConfig) ->
                        explorerMonitorCoroutineScope.launch {
                            try {
                                logger.info { "($networkKey/$explorerKey) Monitoring..." }
                                val record = explorerService.getExplorerState(
                                    networkId = networkKey,
                                    explorerId = explorerKey,
                                    explorerConfig = explorerConfig
                                )
                                explorerStateRepository.create(
                                    networkId = networkKey,
                                    explorerId = explorerKey,
                                    host = explorerConfig.url,
                                    monitor = record
                                )
                                logger.info { "($networkKey/$explorerKey) Added new explorer state" }
                            } catch (exception: Exception) {
                                logger.debugWarn(exception) { "($networkKey/$explorerKey) Failed to monitor" }
                            }
                        }
                    }
                } catch (exception: Exception) {
                    logger.debugWarn(exception) { "($networkKey) Failed to monitor" }
                }
            }
        }
    }
    
    fun stop() {
        monitorServiceCoroutineScope.cancel()
        minerMonitorCoroutineScope.cancel()
        nodecoreMonitorCoroutineScope.cancel()
        altDaemonMonitorCoroutineScope.cancel()
        abfiMonitorCoroutineScope.cancel()
        explorerMonitorCoroutineScope.cancel()
    }
}