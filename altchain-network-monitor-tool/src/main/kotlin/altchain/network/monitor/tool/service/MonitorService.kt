package altchain.network.monitor.tool.service

import altchain.network.monitor.tool.NetworkConfig
import altchain.network.monitor.tool.persistence.repositories.AbfiMonitorRepository
import altchain.network.monitor.tool.persistence.repositories.AltDaemonMonitorRepository
import altchain.network.monitor.tool.persistence.repositories.ExplorerMonitorRepository
import altchain.network.monitor.tool.persistence.repositories.MinerMonitorRepository
import altchain.network.monitor.tool.persistence.repositories.NodeCoreMonitorRepository
import altchain.network.monitor.tool.persistence.repositories.PopSubsidiesRepository
import altchain.network.monitor.tool.persistence.repositories.VbfiMonitorRepository
import altchain.network.monitor.tool.service.abfi.AbfiService
import altchain.network.monitor.tool.service.altchain.AltchainService
import altchain.network.monitor.tool.service.nodecore.NodeCoreService
import altchain.network.monitor.tool.service.popsubsidies.PopSubsidiesService
import altchain.network.monitor.tool.service.vbfi.VbfiService
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
    private val vbfiService: VbfiService,
    private val popSubsidiesService: PopSubsidiesService,
    private val minerMonitorRepository: MinerMonitorRepository,
    private val explorerStateRepository: ExplorerMonitorRepository,
    private val nodeCoreMonitorRepository: NodeCoreMonitorRepository,
    private val abfiMonitorRepository: AbfiMonitorRepository,
    private val vbfiMonitorRepository: VbfiMonitorRepository,
    private val altDaemonMonitorRepository: AltDaemonMonitorRepository,
    private val popSubsidiesRepository: PopSubsidiesRepository,
    private val altchainService: AltchainService,
    private val minerService: MinerService,
    private val explorerService: ExplorerService,
    private val networkConfigs: Map<String, NetworkConfig>
) {
    private val monitorServiceExecutor = createMultiThreadExecutor("monitor-service-thread", 16)
    private val monitorServiceCoroutineScope = CoroutineScope(monitorServiceExecutor.asCoroutineDispatcher())

    private val minerMonitorExecutor = createMultiThreadExecutor("miner-monitor-thread", 4)
    private val minerMonitorCoroutineScope = CoroutineScope(minerMonitorExecutor.asCoroutineDispatcher())

    private val popSubsidiesMonitorExecutor = createMultiThreadExecutor("pop-subsidies-monitor-thread", 4)
    private val popSubsidiesMonitorCoroutineScope = CoroutineScope(popSubsidiesMonitorExecutor.asCoroutineDispatcher())

    private val nodecoreMonitorExecutor = createMultiThreadExecutor("nodecore-monitor-thread", 4)
    private val nodecoreMonitorCoroutineScope = CoroutineScope(nodecoreMonitorExecutor.asCoroutineDispatcher())

    private val altDaemonMonitorExecutor = createMultiThreadExecutor("alt-daemon-monitor-thread", 4)
    private val altDaemonMonitorCoroutineScope = CoroutineScope(altDaemonMonitorExecutor.asCoroutineDispatcher())

    private val abfiMonitorExecutor = createMultiThreadExecutor("abfi-monitor-thread", 4)
    private val abfiMonitorCoroutineScope = CoroutineScope(abfiMonitorExecutor.asCoroutineDispatcher())

    private val vbfiMonitorExecutor = createMultiThreadExecutor("vbfi-monitor-thread", 4)
    private val vbfiMonitorCoroutineScope = CoroutineScope(vbfiMonitorExecutor.asCoroutineDispatcher())

    private val explorerMonitorExecutor = createMultiThreadExecutor("explorer-monitor-thread", 4)
    private val explorerMonitorCoroutineScope = CoroutineScope(explorerMonitorExecutor.asCoroutineDispatcher())

    fun start() {
        networkConfigs.entries.forEach { (networkKey, networkConfig) ->
            monitorServiceCoroutineScope.launchWithFixedDelay(
                initialDelayMillis = TimeUnit.SECONDS.toMillis(1),
                periodMillis = TimeUnit.MINUTES.toMillis(networkConfig.checkDelay)
            ) {
                try {
                    // Miners
                    networkConfig.miners.forEach { (id, config) ->
                        minerMonitorCoroutineScope.launch {
                            try {
                                logger.info { "($networkKey/$id) Monitoring..." }
                                val record = minerService.getMinerMonitor(
                                    networkId = networkKey,
                                    id = id,
                                    config = config
                                )
                                minerMonitorRepository.create(
                                    networkId = networkKey,
                                    id = id,
                                    host = config.apiUrl,
                                    type = config.type,
                                    monitor = record
                                )
                                logger.info { "($networkKey/$id) Added a new state" }
                            } catch (exception: Exception) {
                                logger.debugWarn(exception) { "($networkKey/$id) Failed to monitor" }
                            }
                        }
                    }

                    // NodeCores
                    networkConfig.nodecores.forEach { (id, config) ->
                        nodecoreMonitorCoroutineScope.launch {
                            try {
                                logger.info { "($networkKey/$id) Monitoring..." }
                                val record = nodeCoreService.getMonitor(
                                    networkId = networkKey,
                                    id = id,
                                    config = config
                                )
                                nodeCoreMonitorRepository.create(
                                    networkId = networkKey,
                                    id = id,
                                    host = config.host,
                                    monitor = record
                                )
                                logger.info { "($networkKey/$id) Added a new state" }
                            } catch (exception: Exception) {
                                logger.debugWarn(exception) { "($networkKey/$id) Failed to monitor" }
                            }
                        }
                    }

                    // Alt Daemons
                    networkConfig.altDaemons.forEach { (id, config) ->
                        altDaemonMonitorCoroutineScope.launch {
                            try {
                                logger.info { "($networkKey/$id) Monitoring..." }
                                val record = altchainService.getMonitor(config.siKey)
                                altDaemonMonitorRepository.create(
                                    networkId = networkKey,
                                    id = id,
                                    host = record.host,
                                    monitor = record
                                )
                                logger.info { "($networkKey/$id) Added a new state" }
                            } catch (exception: Exception) {
                                logger.debugWarn(exception) { "($networkKey/$id) Failed to monitor" }
                            }
                        }
                    }

                    // ABFIs
                    networkConfig.abfis.forEach { (id, config) ->
                        abfiMonitorCoroutineScope.launch {
                            try {
                                logger.info { "($networkKey/$id) Monitoring..." }
                                val record = abfiService.getMonitor(
                                    networkId = networkKey,
                                    id = id,
                                    config = config
                                )
                                abfiMonitorRepository.create(
                                    networkId = networkKey,
                                    id = id,
                                    host = config.apiUrl,
                                    prefix = config.prefix,
                                    monitor = record
                                )
                                logger.info { "($networkKey/$id) Added new abfi state" }
                            } catch (exception: Exception) {
                                logger.debugWarn(exception) { "($networkKey/$id) Failed to monitor" }
                            }
                        }
                    }

                    // VBFIs
                    networkConfig.vbfis.forEach { (id, config) ->
                        vbfiMonitorCoroutineScope.launch {
                            try {
                                logger.info { "($networkKey/$id) Monitoring..." }
                                val record = vbfiService.getMonitor(
                                    networkId = networkKey,
                                    id = id,
                                    config = config
                                )
                                vbfiMonitorRepository.create(
                                    networkId = networkKey,
                                    id = id,
                                    host = config.apiUrl,
                                    monitor = record
                                )
                                logger.info { "($networkKey/$id) Added new vbfi state" }
                            } catch (exception: Exception) {
                                logger.debugWarn(exception) { "($networkKey/$id) Failed to monitor" }
                            }
                        }
                    }

                    // Pop Subsidies
                    networkConfig.popSubsidies.forEach { (id, config) ->
                        popSubsidiesMonitorCoroutineScope.launch {
                            try {
                                val record = popSubsidiesService.getMonitor(
                                    networkId = networkKey,
                                    id = id,
                                    config = config
                                )
                                popSubsidiesRepository.create(
                                    networkId = networkKey,
                                    id = id,
                                    host = config.apiUrl,
                                    monitor = record
                                )
                                logger.info { "($networkKey/$id) Added new pop subsidies state" }
                            } catch(exception: Exception) {
                                logger.debugWarn(exception) { "($networkKey/$id) Failed to monitor" }
                            }
                        }
                    }

                    // Explorers
                    networkConfig.explorers.forEach { (id, config) ->
                        explorerMonitorCoroutineScope.launch {
                            try {
                                logger.info { "($networkKey/$id) Monitoring..." }
                                val record = explorerService.getExplorerState(
                                    networkId = networkKey,
                                    id = id,
                                    config = config
                                )
                                explorerStateRepository.create(
                                    networkId = networkKey,
                                    id = id,
                                    host = config.url,
                                    monitor = record
                                )
                                logger.info { "($networkKey/$id) Added new explorer state" }
                            } catch (exception: Exception) {
                                logger.debugWarn(exception) { "($networkKey/$id) Failed to monitor" }
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
        vbfiMonitorCoroutineScope.cancel()
        explorerMonitorCoroutineScope.cancel()
        popSubsidiesMonitorCoroutineScope.cancel()
    }
}