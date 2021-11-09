package altchain.network.monitor.tool.service

import altchain.network.monitor.tool.NetworkConfig
import altchain.network.monitor.tool.api.controller.AbfiBlockInfoResponse
import altchain.network.monitor.tool.api.controller.NetworkStatus
import altchain.network.monitor.tool.service.metrics.Metrics
import altchain.network.monitor.tool.util.createLogger
import altchain.network.monitor.tool.util.createMultiThreadExecutor
import altchain.network.monitor.tool.util.debugWarn
import altchain.network.monitor.tool.util.launchWithFixedDelay
import altchain.network.monitor.tool.util.toInt
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel

private val logger = createLogger {}

class MetricsService(
    private val networkConfigs: Map<String, NetworkConfig>,
    private val networkStatus: NetworkStatus
) {
    private val metricsServiceExecutor = createMultiThreadExecutor("metrics-service-thread", 16)
    private val metricsServiceCoroutineScope = CoroutineScope(metricsServiceExecutor.asCoroutineDispatcher())

    fun start() {
        for ((networkId, networkConfig) in networkConfigs) {
            metricsServiceCoroutineScope.launchWithFixedDelay(
                initialDelayMillis = TimeUnit.SECONDS.toMillis(10),
                periodMillis = TimeUnit.SECONDS.toMillis(30)
            ) {
                logger.trace { "($networkId) Updating metrics..." }
                try {
                    val response = networkStatus.computeNetworkStatus(networkId, networkConfig)
                    Metrics.updateGauge(
                        systemName = "network",
                        networkName = networkId,
                        instanceName = "global",
                        name = "health",
                        value = response.isHealthy.isHealthy.toInt()
                    )
                    Metrics.updateGauge(
                        systemName = "nodecore",
                        networkName = networkId,
                        instanceName = "global",
                        name = "health",
                        value = response.networkNodeCoreMonitor?.isHealthy.toInt()
                    )
                    Metrics.updateGauge(
                        systemName = "altdaemon",
                        networkName = networkId,
                        instanceName = "global",
                        name = "health",
                        value = response.networkAltDaemonMonitor?.isHealthy.toInt()
                    )
                    Metrics.updateGauge(
                        systemName = "explorer",
                        networkName = networkId,
                        instanceName = "global",
                        name = "health",
                        value = response.networkExplorerMonitor?.isHealthy.toInt()
                    )
                    Metrics.updateGauge(
                        systemName = "abfi",
                        networkName = networkId,
                        instanceName = "global",
                        name = "health",
                        value = response.networkAbfiMonitor?.isHealthy.toInt()
                    )
                    Metrics.updateGauge(
                        systemName = "vpm",
                        networkName = networkId,
                        instanceName = "global",
                        name = "health",
                        value = response.networkMinerMonitor?.isVpmHealthy.toInt()
                    )
                    Metrics.updateGauge(
                        systemName = "apm",
                        networkName = networkId,
                        instanceName = "global",
                        name = "health",
                        value = response.networkMinerMonitor?.isApmHealthy.toInt()
                    )
                    networkConfig.nodecores.keys.forEach { id ->
                        val nodeCoreMonitorResponse = response.networkNodeCoreMonitor?.nodeCoreMonitors?.find { it.nodecoreId == id }
                        Metrics.updateGauge(
                            systemName = "nodecore",
                            networkName = networkId,
                            instanceName = id,
                            name = "network_height",
                            value = nodeCoreMonitorResponse?.networkHeight ?: 0
                        )
                        Metrics.updateGauge(
                            systemName = "nodecore",
                            networkName = networkId,
                            instanceName = id,
                            name = "local_height",
                            value = nodeCoreMonitorResponse?.localHeight ?: 0
                        )
                        Metrics.updateGauge(
                            systemName = "nodecore",
                            networkName = networkId,
                            instanceName = id,
                            name = "block_difference",
                            value = nodeCoreMonitorResponse?.blockDifference ?: 0
                        )
                        Metrics.updateGauge(
                            systemName = "nodecore",
                            networkName = networkId,
                            instanceName = id,
                            name = "health",
                            value = nodeCoreMonitorResponse?.isHealthy?.isHealthy.toInt()
                        )
                    }
                    networkConfig.altDaemons.keys.forEach { id ->
                        val altDaemonMonitorResponse = response.networkAltDaemonMonitor?.altDaemonMonitors?.find { it.altDaemonId == id }
                        Metrics.updateGauge(
                            systemName = "altdaemon",
                            networkName = networkId,
                            instanceName = id,
                            name = "network_height",
                            value = altDaemonMonitorResponse?.networkHeight ?: 0
                        )
                        Metrics.updateGauge(
                            systemName = "altdaemon",
                            networkName = networkId,
                            instanceName = id,
                            name = "local_height",
                            value = altDaemonMonitorResponse?.localHeight ?: 0
                        )
                        Metrics.updateGauge(
                            systemName = "altdaemon",
                            networkName = networkId,
                            instanceName = id,
                            name = "block_difference",
                            value = altDaemonMonitorResponse?.blockDifference ?: 0
                        )
                        Metrics.updateGauge(
                            systemName = "altdaemon",
                            networkName = networkId,
                            instanceName = id,
                            name = "health",
                            value = altDaemonMonitorResponse?.isHealthy?.isHealthy.toInt()
                        )
                    }
                    networkConfig.explorers.keys.forEach { id ->
                        val explorerMonitorResponse = response.networkExplorerMonitor?.explorerMonitors?.find { it.explorerId == id }
                        Metrics.updateGauge(
                            systemName = "explorer",
                            networkName = networkId,
                            instanceName = id,
                            name = "atvs",
                            value = explorerMonitorResponse?.atvCount ?: 0
                        )
                        Metrics.updateGauge(
                            systemName = "explorer",
                            networkName = networkId,
                            instanceName = id,
                            name = "vtbs",
                            value = explorerMonitorResponse?.vtbCount ?: 0
                        )
                        Metrics.updateGauge(
                            systemName = "explorer",
                            networkName = networkId,
                            instanceName = id,
                            name = "vbks",
                            value = explorerMonitorResponse?.vbkCount ?: 0
                        )
                        Metrics.updateGauge(
                            systemName = "explorer",
                            networkName = networkId,
                            instanceName = id,
                            name = "health",
                            value = explorerMonitorResponse?.isHealthy?.isHealthy.toInt()
                        )
                    }
                    networkConfig.miners.forEach { (id, minerConfig) ->
                        val minerMonitorResponse = response.networkMinerMonitor?.minerMonitors?.find {
                            it.minerId == id && it.minerType == minerConfig.type
                        }
                        Metrics.updateGauge(
                            systemName = "miner",
                            networkName = networkId,
                            instanceName = id,
                            extraTags = listOf("type" to minerConfig.type.name),
                            name = "started_operations",
                            value = minerMonitorResponse?.startedOperationCount ?: 0
                        )
                        Metrics.updateGauge(
                            systemName = "miner",
                            networkName = networkId,
                            instanceName = id,
                            extraTags = listOf("type" to minerConfig.type.name),
                            name = "completed_operations",
                            value = minerMonitorResponse?.completedOperationCount ?: 0
                        )
                        Metrics.updateGauge(
                            systemName = "miner",
                            networkName = networkId,
                            instanceName = id,
                            extraTags = listOf("type" to minerConfig.type.name),
                            name = "failed_operations",
                            value = minerMonitorResponse?.failedOperationCount ?: 0
                        )
                        Metrics.updateGauge(
                            systemName = "miner",
                            networkName = networkId,
                            instanceName = id,
                            extraTags = listOf("type" to minerConfig.type.name),
                            name = "health",
                            value = minerMonitorResponse?.isHealthy?.isHealthy.toInt()
                        )
                    }
                    networkConfig.abfis.keys.forEach { id ->
                        val abfi = response.networkAbfiMonitor?.abfiMonitors?.find {
                            it.abfiId == id
                        }
                        Metrics.updateGauge(
                            systemName = "abfi",
                            networkName = networkId,
                            instanceName = id,
                            name = "health",
                            value = abfi?.isHealthy?.isHealthy.toInt()
                        )
                        abfi?.blockInfo?.blocks?.forEach { abfiBlockInfoSummaryResponse ->
                            abfiBlockInfoSummaryResponse.blockInfo.updateGauges(
                                networkId = networkId,
                                id = id,
                                name = abfiBlockInfoSummaryResponse.name
                            )
                        }
                    }
                    logger.trace { "($networkId) Added new metrics" }
                } catch (exception: Exception) {
                    logger.debugWarn(exception) { "($networkId) Failed to generate metrics" }
                }
            }
        }
    }

    private fun AbfiBlockInfoResponse?.updateGauges(networkId: String, id: String, name: String) {
        Metrics.updateGauge("abfi", networkId, id, name + "Height", this?.height ?: -1)
        Metrics.updateGauge("abfi", networkId, id, name + "SpFinality", this?.spFinality ?: -1)
        Metrics.updateGauge("abfi", networkId, id, name + "BitcoinFinality", this?.bitcoinFinality ?: -1)
        Metrics.updateGauge("abfi", networkId, id, name + "EndorsedIn", this?.endorsedInHeight ?: -1)
        Metrics.updateGauge("abfi", networkId, id, name + "VerifiedIn", this?.verifiedInHeight ?: -1)
    }

    fun stop() {
        metricsServiceCoroutineScope.cancel()
    }
}