package altchain.network.monitor.tool.api.controller

import altchain.network.monitor.tool.NetworkConfig
import altchain.network.monitor.tool.api.BadRequestException
import altchain.network.monitor.tool.persistence.repositories.AbfiMonitorRepository
import altchain.network.monitor.tool.persistence.repositories.AltDaemonMonitorRepository
import altchain.network.monitor.tool.persistence.repositories.ExplorerMonitorRepository
import altchain.network.monitor.tool.persistence.repositories.MinerMonitorRepository
import altchain.network.monitor.tool.persistence.repositories.NodeCoreMonitorRepository
import altchain.network.monitor.tool.persistence.repositories.PopSubsidiesRepository
import altchain.network.monitor.tool.persistence.repositories.VbfiMonitorRepository
import altchain.network.monitor.tool.persistence.tables.AbfiBlockInfoRecord
import altchain.network.monitor.tool.persistence.tables.AbfiBlockRecord
import altchain.network.monitor.tool.persistence.tables.AbfiMonitorRecord
import altchain.network.monitor.tool.persistence.tables.AltDaemonMonitorRecord
import altchain.network.monitor.tool.persistence.tables.ExplorerMonitorRecord
import altchain.network.monitor.tool.persistence.tables.MetricRecord
import altchain.network.monitor.tool.persistence.tables.MinerMonitorRecord
import altchain.network.monitor.tool.persistence.tables.MinerType
import altchain.network.monitor.tool.persistence.tables.NodeCoreMonitorRecord
import altchain.network.monitor.tool.persistence.tables.PopSubsidiesMonitorRecord
import altchain.network.monitor.tool.persistence.tables.VbfiMonitorRecord
import altchain.network.monitor.tool.service.altchain.AltchainService
import altchain.network.monitor.tool.util.now
import altchain.network.monitor.tool.util.toLowerCase
import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.auth.OpenAPIAuthenticatedRoute
import com.papsign.ktor.openapigen.route.path.auth.get
import com.papsign.ktor.openapigen.route.response.respond
import io.ktor.auth.UserIdPrincipal
import com.papsign.ktor.openapigen.route.route
import java.time.Duration
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlin.math.abs

private const val defaultDownMessage = "\$id is configured but the tool doesn't have any data about it, might be down or inaccessible"
private const val defaultOldDataMessage = "\$id monitor data is too old, the latest collected data is from \$timeDifference minutes ago, it might be down or inaccessible"
private const val defaultNotSyncNodeMessage = "\$id is not synchronized, there are \$blockDifference blocks of difference between the local height (\$localHeight) and the network height (\$networkHeight)"

class NetworkStatus(
    private val networkConfigs: Map<String, NetworkConfig>,
    private val abfiMonitorRepository: AbfiMonitorRepository,
    private val vbfiMonitorRepository: VbfiMonitorRepository,
    private val nodeCoreMonitorRepository: NodeCoreMonitorRepository,
    private val explorerMonitorRepository: ExplorerMonitorRepository,
    private val altDaemonMonitorRepository: AltDaemonMonitorRepository,
    private val altchainService: AltchainService,
    private val minerMonitorRepository: MinerMonitorRepository,
    private val popSubsidiesRepository: PopSubsidiesRepository
) : ApiController {

    @Path("")
    data class NetworkStatusPath(
        @QueryParam("Network Id") val networkId: String
    )

    override fun OpenAPIAuthenticatedRoute<UserIdPrincipal>.registerApi() = route("network-status") {
        get<NetworkStatusPath, NetworkMonitorResponse, UserIdPrincipal>(
            info("Get the network status for the given network id")
        ) { location ->
            val network = networkConfigs[location.networkId]
                ?: throw BadRequestException("${location.networkId} is not a valid network id")
            respond(computeNetworkStatus(location.networkId, network))
        }
        route("all").get<Unit, NetworkMonitorSummaryResponse, UserIdPrincipal>(
            info("Get the status from all the configured networks")
        ) {
            val networks = networkConfigs.entries.map {
                computeNetworkStatus(it.key, it.value)
            }
            respond(NetworkMonitorSummaryResponse(networks))
        }
    }

    suspend fun computeNetworkStatus(networkId: String, network: NetworkConfig): NetworkMonitorResponse {
        val networkNodeCoreMonitorResponse = if (network.nodecores.isNotEmpty()) {
            val monitors = nodeCoreMonitorRepository.find(
                networkId = networkId,
                ids = network.nodecores.keys.toLowerCase()
            )
            val response = monitors.map { record ->
                record.toNodeCoreMonitorResponse(network.maxHealthyByTime)
            }
            val notPresentResponse = network.nodecores.filterNot { entry ->
                monitors.any { monitor ->
                    monitor.id == entry.key
                }
            }.map {
                notPresentNodeCoreMonitorResponse(
                    networkId = networkId,
                    id = it.key,
                    host = it.value.host
                )
            }
            (response + notPresentResponse).toNetworkNodeCoreMonitorResponse(
                minPercentageHealthy = network.minPercentageHealthyNodeCores,
                total = network.nodecores.size
            )
        } else {
            null
        }

        val networkAltDaemonMonitorResponse = if (network.altDaemons.isNotEmpty()) {
            val monitors = altDaemonMonitorRepository.find(
                networkId = networkId,
                ids = network.altDaemons.keys.toLowerCase()
            )
            val response = monitors.map { record ->
                record.toAltDaemonMonitorResponse(network.maxHealthyByTime)
            }
            val notPresentResponse = network.altDaemons.filterNot { entry ->
                monitors.any { monitor ->
                    monitor.id == entry.key
                }
            }.map {
                notPresentAltDaemonMonitorResponse(
                    networkId = networkId,
                    id = it.key,
                    host = altchainService.getPluginByKey(it.key)?.config?.host ?: "Not Found"
                )
            }
            (notPresentResponse + response).toNetworkAltDaemonMonitorResponse(
                minPercentageHealthy = network.minPercentageHealthyAltDaemons,
                total = network.altDaemons.size
            )
        } else {
            null
        }
        val networkExplorerMonitorResponse = if (network.explorers.isNotEmpty()) {
            val monitors = explorerMonitorRepository.find(
                networkId = networkId,
                ids = network.explorers.keys.toLowerCase()
            )
            val response = monitors.map { record ->
                record.toExplorerMonitorResponse(network.maxHealthyByTime)
            }
            val notPresentResponse = network.explorers.filterNot { entry ->
                monitors.any { monitor ->
                    monitor.id == entry.key
                }
            }.map {
                notPresentExplorerMonitorResponse(
                    networkId = networkId,
                    id = it.key,
                    host = it.value.url
                )
            }
            (notPresentResponse + response).toNetworkExplorerMonitorResponse(
                minPercentageHealthy = network.minPercentageHealthyExplorers,
                total = network.explorers.size
            )
        } else {
            null
        }

        val networkAbfiMonitorResponse = if (network.abfis.isNotEmpty()) {
            val monitors = abfiMonitorRepository.find(
                networkId = networkId,
                ids = network.abfis.keys.toLowerCase()
            )
            val response = monitors.map { record ->
                record.toAbfiMonitorResponse(network.maxHealthyByTime)
            }
            val notPresentResponse = network.abfis.filterNot { entry ->
                monitors.any { monitor ->
                    monitor.abfiId == entry.key
                }
            }.map {
                notPresentAbfiMonitorResponse(
                    networkId = networkId,
                    id = it.key,
                    host = it.value.apiUrl,
                    prefix = it.value.prefix
                )
            }
            (notPresentResponse + response).toNetworkAbfiMonitorResponse(
                minPercentageHealthy = network.minPercentageHealthyAbfis,
                total = network.abfis.size
            )
        } else {
            null
        }

        val networkVbfiMonitorResponse = if (network.vbfis.isNotEmpty()) {
            val monitors = vbfiMonitorRepository.find(
                networkId = networkId,
                ids = network.vbfis.keys.toLowerCase()
            )
            val response = monitors.map { record ->
                record.toVbfiMonitorResponse(network.maxHealthyByTime)
            }
            val notPresentResponse = network.vbfis.filterNot { entry ->
                monitors.any { monitor ->
                    monitor.id == entry.key
                }
            }.map {
                notPresentVbfiMonitorResponse(
                    networkId = networkId,
                    id = it.key,
                    host = it.value.apiUrl
                )
            }
            (notPresentResponse + response).toNetworkVbfiMonitorResponse(
                minPercentageHealthy = network.minPercentageHealthyVbfis,
                total = network.vbfis.size
            )
        } else {
            null
        }

        val networkMinerMonitorResponse = if (network.miners.isNotEmpty()) {
            val monitors = minerMonitorRepository.find(
                networkId = networkId,
                ids = network.miners.keys.toLowerCase()
            )
            val response = monitors.map { record ->
                record.toMinerMonitorResponse(
                    maxHealthyByTime = network.maxHealthyByTime,
                    operationsThreshold = if (record.minerType == MinerType.VPM) {
                        network.maxPercentageNotHealthyVpmOperations
                    } else {
                        network.maxPercentageNotHealthyApmOperations
                    },
                    verifyIsMining = network.miners[record.id]?.verifyIsMining ?: false
                )
            }
            val notPresentResponse = network.miners.filterNot { entry ->
                monitors.any { monitor ->
                    monitor.id == entry.key
                }
            }.map {
                notPresentMinerMonitorResponse(
                    networkId = networkId,
                    id = it.key,
                    host = it.value.apiUrl,
                    minerType = it.value.type
                )
            }
            (notPresentResponse + response).toNetworkMinerMonitorResponse(
                minPercentageHealthyVpms = network.minPercentageHealthyVpms,
                minPercentageHealthyApms = network.minPercentageHealthyApms,
                totalVpms = network.miners.values.count { it.type == MinerType.VPM },
                totalApms = network.miners.values.count { it.type == MinerType.APM }
            )
        } else {
            null
        }

        val networkPopSubsidiesMonitorResponse = if (network.popSubsidies.isNotEmpty()) {
            val monitors = popSubsidiesRepository.find(
                networkId = networkId,
                ids = network.popSubsidies.keys.toLowerCase()
            )
            val response = monitors.map { record ->
                record.toPopSubsidiesMonitorResponse(network.maxHealthyByTime)
            }
            val notPresentResponse = network.popSubsidies.filterNot { entry ->
                monitors.any { monitor ->
                    monitor.id == entry.key
                }
            }.map {
                notPresentPopSubsidiesMonitorResponse(
                    networkId = networkId,
                    id = it.key,
                    host = it.value.apiUrl
                )
            }
            (notPresentResponse + response).toNetworkPopSubsidiesMonitorResponse(
                minPercentageHealthy = network.minPercentageHealthyPopSubsidies,
                total = network.popSubsidies.size
            )
        } else {
            null
        }

        val isHealthy = networkNodeCoreMonitorResponse?.isHealthy ?: true &&
                networkAltDaemonMonitorResponse?.isHealthy ?: true &&
                networkExplorerMonitorResponse?.isHealthy ?: true &&
                networkAbfiMonitorResponse?.isHealthy ?: true &&
                networkVbfiMonitorResponse?.isHealthy ?: true &&
                networkMinerMonitorResponse?.isVpmHealthy ?: true &&
                networkMinerMonitorResponse?.isApmHealthy ?: true &&
                networkPopSubsidiesMonitorResponse?.isHealthy ?: true

        val nodeCoreDiagnostics = networkNodeCoreMonitorResponse?.monitors?.filter {
            !it.isHealthy.isHealthy
        }?.mapNotNull { response ->
            response.isHealthy.reason
        } ?: emptyList()

        val altDaemonDiagnostics = networkAltDaemonMonitorResponse?.monitors?.filter {
            !it.isHealthy.isHealthy
        }?.mapNotNull { response ->
            response.isHealthy.reason
        } ?: emptyList()

        val explorerDiagnostics = networkExplorerMonitorResponse?.monitors?.filter {
            !it.isHealthy.isHealthy
        }?.mapNotNull { response ->
            response.isHealthy.reason
        } ?: emptyList()

        val abfiDiagnostics = networkAbfiMonitorResponse?.monitors?.filter {
            !it.isHealthy.isHealthy
        }?.mapNotNull { response ->
            response.isHealthy.reason
        } ?: emptyList()

        val vbfiDiagnostics = networkVbfiMonitorResponse?.monitors?.filter {
            !it.isHealthy.isHealthy
        }?.mapNotNull { response ->
            response.isHealthy.reason
        } ?: emptyList()

        val popSubsidiesDiagnostics = networkPopSubsidiesMonitorResponse?.monitors?.filter {
            !it.isHealthy.isHealthy
        }?.mapNotNull { response ->
            response.isHealthy.reason
        } ?: emptyList()

        val minerDiagnostics = networkMinerMonitorResponse?.monitors?.filter {
            !it.isHealthy.isHealthy
        }?.mapNotNull { response ->
            response.isHealthy.reason
        } ?: emptyList()

        val response = NetworkMonitorResponse(
            networkId = networkId,
            isHealthy = HealthyStatusReportResponse(
                isHealthy = isHealthy,
                diagnostics = (nodeCoreDiagnostics + altDaemonDiagnostics + explorerDiagnostics + abfiDiagnostics + minerDiagnostics + vbfiDiagnostics + popSubsidiesDiagnostics).ifEmpty {
                    null
                }
            ),
            networkNodeCoreMonitor = networkNodeCoreMonitorResponse,
            networkAltDaemonMonitor = networkAltDaemonMonitorResponse,
            networkExplorerMonitor = networkExplorerMonitorResponse,
            networkAbfiMonitor = networkAbfiMonitorResponse,
            networkVbfiMonitor = networkVbfiMonitorResponse,
            networkMinerMonitor = networkMinerMonitorResponse,
            networkPopSubsidiesMonitor = networkPopSubsidiesMonitorResponse
        )
        return response
    }
}

private fun MinerMonitorRecord.toMinerMonitorResponse(
    maxHealthyByTime: Int,
    operationsThreshold: Int,
    verifyIsMining: Boolean
) : MinerMonitorResponse {
    val timeDifference = Duration.between(addedAt.toJavaInstant(), now().toJavaInstant()).toMinutes()
    val isHealthyByTime = timeDifference <= maxHealthyByTime
    val percentage = getHealthyPercentage(
        count = failedOperationCount,
        totalCount = startedOperationCount
    )
    val isHealthyByOperations = percentage < operationsThreshold
    val isHealthyByOperationsReport = if (!isHealthyByOperations) {
        "$id operations are failing beyond the threshold ($operationsThreshold%), started operations: $startedOperationCount, completed operations: $completedOperationCount, failed operations: $failedOperationCount"
    } else {
        null
    }
    val isHealthyByTimeReport = if (!isHealthyByTime) {
        defaultOldDataMessage.replace("\$id", id).replace("\$timeDifference", "$timeDifference")
    } else {
        null
    }
    val isHealthyByMining = if (verifyIsMining && !isMining) {
        "$id is not creating new operations"
    } else {
        null
    }
    return MinerMonitorResponse(
        networkId = networkId,
        id = id,
        version = version,
        host = host,
        type = minerType,
        startedOperationCount = startedOperationCount,
        completedOperationCount = completedOperationCount,
        failedOperationCount = failedOperationCount,
        verifyIsMining = verifyIsMining,
        isMining = isMining,
        isHealthyByTime = isHealthyByTime,
        isHealthyByOperations = isHealthyByOperations,
        isHealthy = HealthyStatusResponse(
            isHealthy = isHealthyByTime && isHealthyByOperations && ((verifyIsMining && isMining) || (!verifyIsMining)),
            reason = listOfNotNull(isHealthyByTimeReport, isHealthyByMining, isHealthyByOperationsReport).firstOrNull()
        ),
        metrics = metrics.metrics.map {
            it.toMetricResponse()
        },
        uptimeSeconds = uptimeSeconds,
        addedAt = addedAt
    )
}

private fun AbfiMonitorRecord.toAbfiMonitorResponse(
    maxHealthyByTime: Int
): AbfiMonitorResponse {
    val timeDifference = Duration.between(addedAt.toJavaInstant(), now().toJavaInstant()).toMinutes()
    val isHealthyByTime = timeDifference <= maxHealthyByTime
    val isHealthyByLastFinalizedBlockBtcReport = if (!haveLastFinalizedBlockBtc) {
        "$abfiId last finalized btc is not present. $diagnostic"
    } else {
        null
    }
    val blockDifference = abs(lastNetworkBlockHeight - lastFinalizedBlockHeight)
    val isHealthyByBlocksReport = if (!isSynchronized) {
        defaultNotSyncNodeMessage.replace("\$id", abfiId).replace("\$blockDifference", "$blockDifference")
            .replace("\$localHeight", "$lastFinalizedBlockHeight").replace("\$networkHeight", "$lastNetworkBlockHeight")
    } else {
        null
    }
    val isHealthyByTimeReport = if (!isHealthyByTime) {
        "${defaultOldDataMessage.replace("\$id", abfiId).replace("\$timeDifference", "$timeDifference")}, diagnostic: $diagnostic"
    } else {
        null
    }
    return AbfiMonitorResponse(
        networkId = networkId,
        id = abfiId,
        version = abfiVersion,
        host = host,
        prefix = prefix,
        blockInfo = AbfiBlockSummaryResponse(
            blocks = blockInfo.blocks.map {
                it.toAbfiBlockInfoSummaryResponse()
            }
        ),
        lastFinalizedBlockHeight = lastFinalizedBlockHeight,
        lastNetworkBlockHeight = lastNetworkBlockHeight,
        blockDifference = blockDifference,
        isHealthyByTime = isHealthyByTime,
        isHealthyByLastFinalizedBlockBtc = haveLastFinalizedBlockBtc,
        isHealthyByBlocks = isSynchronized,
        isHealthy = HealthyStatusResponse(
            isHealthy = isHealthyByTime && haveLastFinalizedBlockBtc && isSynchronized,
            reason = listOfNotNull(isHealthyByTimeReport, isHealthyByLastFinalizedBlockBtcReport, isHealthyByBlocksReport).firstOrNull()
        ),
        addedAt = addedAt
    )
}

private fun VbfiMonitorRecord.toVbfiMonitorResponse(
    maxHealthyByTime: Int
): VbfiMonitorResponse {
    val timeDifference = Duration.between(addedAt.toJavaInstant(), now().toJavaInstant()).toMinutes()
    val isHealthyByTime = timeDifference <= maxHealthyByTime

    val lastBlockDifference = abs(lastExplorerBlockHeight - lastBlockHeight)
    val isHealthyByLastBlockReport = if (!isLastBlockSynchronized) {
        defaultNotSyncNodeMessage.replace("\$id", "$id (last block)").replace("\$blockDifference", "$lastBlockDifference")
            .replace("\$localHeight", "$lastBlockHeight").replace("\$networkHeight", "$lastExplorerBlockHeight")
    } else {
        null
    }

    val lastBlockFinalizedBtcDifference = abs(lastExplorerBlockHeight - lastBlockFinalizedBtcHeight)
    val isHealthyByLastBlockFinalizedReport = if (!isLastBlockFinalizedBtcSynchronized) {
        defaultNotSyncNodeMessage.replace("\$id", "$id (last finalized block)").replace("\$blockDifference", "$lastBlockFinalizedBtcDifference")
            .replace("\$localHeight", "$lastBlockFinalizedBtcHeight").replace("\$networkHeight", "$lastExplorerBlockHeight")
    } else {
        null
    }

    val isHealthyByTimeReport = if (!isHealthyByTime) {
        defaultOldDataMessage.replace("\$id", id).replace("\$timeDifference", "$timeDifference")
    } else {
        null
    }
    return VbfiMonitorResponse(
        networkId = networkId,
        id = id,
        version = version,
        host = host,
        lastBlockHeight = lastBlockHeight,
        lastBlockFinalizedBtcHeight = lastBlockFinalizedBtcHeight,
        lastExplorerBlockHeight = lastExplorerBlockHeight,
        lastBlockDifference = lastBlockDifference,
        lastBlockFinalizedBtcDifference = lastBlockFinalizedBtcDifference,
        isHealthyByLastBlock = isLastBlockSynchronized,
        isHealthyByLastFinalizedBlockBtc = isLastBlockFinalizedBtcSynchronized,
        isHealthyByTime = isHealthyByTime,
        isHealthy = HealthyStatusResponse(
            isHealthy = isLastBlockSynchronized && isLastBlockFinalizedBtcSynchronized && isHealthyByTime,
            reason = listOfNotNull(isHealthyByTimeReport, isHealthyByLastBlockReport, isHealthyByLastBlockFinalizedReport).firstOrNull()
        ),
        addedAt = addedAt
    )
}

private fun PopSubsidiesMonitorRecord.toPopSubsidiesMonitorResponse(
    maxHealthyByTime: Int
): PopSubsidiesMonitorResponse {
    val timeDifference = Duration.between(addedAt.toJavaInstant(), now().toJavaInstant()).toMinutes()
    val isHealthyByTime = timeDifference <= maxHealthyByTime
    val isHealthyByTimeReport = if (!isHealthyByTime) {
        defaultOldDataMessage.replace("\$id", id).replace("\$timeDifference", "$timeDifference")
    } else {
        null
    }
    return PopSubsidiesMonitorResponse(
        networkId = networkId,
        id = id,
        version = version,
        host = host,
        startedOperationCount = startedOperationCount,
        completedOperationCount = completedOperationCount,
        failedOperationCount = failedOperationCount,
        isHealthyByTime = isHealthyByTime,
        isHealthy = HealthyStatusResponse(
            isHealthy = isHealthyByTime,
            reason = listOfNotNull(isHealthyByTimeReport).firstOrNull()
        ),
        addedAt = addedAt
    )
}

private fun NodeCoreMonitorRecord.toNodeCoreMonitorResponse(
    maxHealthyByTime: Int
): NodeCoreMonitorResponse {
    val timeDifference = Duration.between(addedAt.toJavaInstant(), now().toJavaInstant()).toMinutes()
    val isHealthyByTime = timeDifference <= maxHealthyByTime
    val blockDifference = abs(localHeight - networkHeight)
    val isHealthyByBlocksReport = if (!isSynchronized) {
        defaultNotSyncNodeMessage.replace("\$id", id).replace("\$blockDifference", "$blockDifference")
            .replace("\$localHeight", "$localHeight").replace("\$networkHeight", "$networkHeight")
    } else {
        null
    }
    val isHealthyByTimeReport = if (!isHealthyByTime) {
        defaultOldDataMessage.replace("\$id", id).replace("\$timeDifference", "$timeDifference")
    } else {
        null
    }
    return NodeCoreMonitorResponse(
        networkId = networkId,
        id = id,
        version = version,
        host = host,
        localHeight = localHeight,
        networkHeight = networkHeight,
        blockDifference = blockDifference,
        isHealthyByBlocks = isSynchronized,
        isHealthyByTime = isHealthyByTime,
        isHealthy = HealthyStatusResponse(
            isHealthy = isSynchronized && isHealthyByTime,
            reason = listOfNotNull(isHealthyByTimeReport, isHealthyByBlocksReport).firstOrNull()
        ),
        addedAt = addedAt
    )
}

private fun AltDaemonMonitorRecord.toAltDaemonMonitorResponse(
    maxHealthyByTime: Int
): AltDaemonMonitorResponse {
    val timeDifference = Duration.between(addedAt.toJavaInstant(), now().toJavaInstant()).toMinutes()
    val isHealthyByTime = timeDifference <= maxHealthyByTime
    val blockDifference = abs(localHeight - networkHeight)
    val isHealthyByBlocksReport = if (!isSynchronized) {
        defaultNotSyncNodeMessage.replace("\$id", id).replace("\$blockDifference", "$blockDifference")
            .replace("\$localHeight", "$localHeight").replace("\$networkHeight", "$networkHeight")
    } else {
        null
    }
    val isHealthyByTimeReport = if (!isHealthyByTime) {
        defaultOldDataMessage.replace("\$id", id).replace("\$timeDifference", "$timeDifference")
    } else {
        null
    }
    return AltDaemonMonitorResponse(
        networkId = networkId,
        altDaemonId = id,
        host = host,
        localHeight = localHeight,
        networkHeight = networkHeight,
        blockDifference = blockDifference,
        isHealthyByBlocks = isSynchronized,
        isHealthyByTime = isHealthyByTime,
        isHealthy = HealthyStatusResponse(
            isHealthy = isSynchronized && isHealthyByTime,
            reason = listOfNotNull(isHealthyByTimeReport, isHealthyByBlocksReport).firstOrNull()
        ),
        addedAt = addedAt
    )
}

private fun AbfiBlockRecord.toAbfiBlockInfoSummaryResponse(): AbfiBlockInfoSummaryResponse {
    return AbfiBlockInfoSummaryResponse(
       name = name,
       blockInfo =  blockInfo.toAbfiBlockInfoResponse()
    )
}

private fun AbfiBlockInfoRecord.toAbfiBlockInfoResponse(): AbfiBlockInfoResponse {
    return AbfiBlockInfoResponse(
        height = height,
        spFinality = spFinality,
        bitcoinFinality = bitcoinFinality,
        endorsedInHeight = endorsedInHeight,
        verifiedInHeight = verifiedInHeight
    )
}

private fun List<AltDaemonMonitorResponse>.toNetworkAltDaemonMonitorResponse(
    minPercentageHealthy: Int,
    total: Int
): NetworkAltDaemonMonitorResponse  {
    val healthyCount = count { it.isHealthy.isHealthy }
    val percentage = getHealthyPercentage(
        count = healthyCount,
        totalCount = total
    )
    val isHealthy = percentage >= minPercentageHealthy
    return NetworkAltDaemonMonitorResponse(
        isHealthy = isHealthy,
        monitors = this
    )
}

private fun ExplorerMonitorRecord.toExplorerMonitorResponse(
    maxHealthyByTime: Int
): ExplorerMonitorResponse {
    val isHealthyByBlocks = atvCount > 0 && vtbCount > 0 && vbkCount > 0
    val timeDifference = Duration.between(addedAt.toJavaInstant(), Clock.System.now().toJavaInstant()).toMinutes()
    val isHealthyByTime = timeDifference <= maxHealthyByTime
    val isHealthyByBlocksReport = if (!isHealthyByBlocks) {
        val blocks = listOf(
            if (atvCount <= 0) "Atvs" else "",
            if (vtbCount <= 0) "Vtbs" else "",
            if (vbkCount <= 0) "Vbks" else "",
        )
        "$id doesn't have any ${blocks.filter { it.isNotEmpty() }.joinToString()}"
    } else {
        null
    }
    val isHealthyByTimeReport = if (!isHealthyByTime) {
        defaultOldDataMessage.replace("\$id", id).replace("\$timeDifference", "$timeDifference")
    } else {
        null
    }
    return ExplorerMonitorResponse(
        networkId = networkId,
        explorerId = id,
        host = host,
        blockCount = blockCount,
        atvCount = atvCount,
        vtbCount = vtbCount,
        vbkCount = vbkCount,
        atvBlocks = atvBlocks,
        vtbBlocks = vtbBlocks,
        vbkBlocks = vbkBlocks,
        isHealthyByBlocks = isHealthyByBlocks,
        isHealthyByTime = isHealthyByTime,
        isHealthy = HealthyStatusResponse(
            isHealthy = isHealthyByBlocks && isHealthyByTime,
            reason = listOfNotNull(isHealthyByTimeReport, isHealthyByBlocksReport).firstOrNull()
        ),
        addedAt = addedAt
    )
}

private fun MetricRecord.toMetricResponse(): MetricResponse = MetricResponse(
    type = type.name,
    value = value
)

private fun getHealthyPercentage(count: Int, totalCount: Int): Int = if (count > 0) {
    ((count.toDouble() / totalCount.toDouble()) * 100).toInt()
} else {
    0
}

private fun notPresentAbfiMonitorResponse(
    networkId: String,
    id: String,
    host: String,
    prefix: String,
): AbfiMonitorResponse {
    return AbfiMonitorResponse(
        networkId = networkId,
        id = id,
        host = host,
        prefix = prefix,
        isHealthy = HealthyStatusResponse(
            isHealthy = false,
            reason = defaultDownMessage.replace("\$id", id)
        )
    )
}

private fun notPresentVbfiMonitorResponse(
    networkId: String,
    id: String,
    host: String
): VbfiMonitorResponse {
    return VbfiMonitorResponse(
        networkId = networkId,
        id = id,
        host = host,
        isHealthy = HealthyStatusResponse(
            isHealthy = false,
            reason = defaultDownMessage.replace("\$id", id)
        )
    )
}

private fun notPresentPopSubsidiesMonitorResponse(
    networkId: String,
    id: String,
    host: String,
): PopSubsidiesMonitorResponse = PopSubsidiesMonitorResponse(
    networkId = networkId,
    id = id,
    host = host,
    isHealthy = HealthyStatusResponse(
        isHealthy = false,
        reason = defaultDownMessage.replace("\$id", id)
    )
)

private fun notPresentExplorerMonitorResponse(
    networkId: String,
    id: String,
    host: String
): ExplorerMonitorResponse {
    return ExplorerMonitorResponse(
        networkId = networkId,
        explorerId = id,
        host = host,
        isHealthy = HealthyStatusResponse(
            isHealthy = false,
            reason = defaultDownMessage.replace("\$id", id)
        )
    )
}

private fun notPresentNodeCoreMonitorResponse(
    networkId: String,
    id: String,
    host: String,
): NodeCoreMonitorResponse {
    return NodeCoreMonitorResponse(
        networkId = networkId,
        id = id,
        host = host,
        isHealthy = HealthyStatusResponse(
            isHealthy = false,
            reason = defaultDownMessage.replace("\$id", id)
        )
    )
}

private fun notPresentAltDaemonMonitorResponse(
    networkId: String,
    id: String,
    host: String,
): AltDaemonMonitorResponse {
    return AltDaemonMonitorResponse(
        networkId = networkId,
        altDaemonId = id,
        host = host,
        isHealthy = HealthyStatusResponse(
            isHealthy = false,
            reason = defaultDownMessage.replace("\$id", id)
        ),
        addedAt = Instant.DISTANT_PAST
    )
}

private fun notPresentMinerMonitorResponse(
    networkId: String,
    id: String,
    host: String,
    minerType: MinerType
): MinerMonitorResponse {
    return MinerMonitorResponse(
        networkId = networkId,
        id = id,
        host = host,
        type = minerType,
        isHealthy = HealthyStatusResponse(
            isHealthy = false,
            reason = defaultDownMessage.replace("\$id", id)
        )
    )
}

private fun List<MinerMonitorResponse>.toNetworkMinerMonitorResponse(
    minPercentageHealthyVpms: Int,
    minPercentageHealthyApms: Int,
    totalVpms: Int,
    totalApms: Int
): NetworkMinerMonitorResponse {
    val healthyVpmCount = asSequence().filter {
        it.type == MinerType.VPM
    }.count {
        it.isHealthy.isHealthy
    }
    val percentageVpm = getHealthyPercentage(
        count = healthyVpmCount,
        totalCount = totalVpms
    )
    val isVpmHealthy = percentageVpm >= minPercentageHealthyVpms
    val healthyApmCount = asSequence().filter {
        it.type == MinerType.APM
    }.count {
        it.isHealthy.isHealthy
    }
    val percentageApm = getHealthyPercentage(
        count = healthyApmCount,
        totalCount = totalApms
    )
    val isApmHealthy = percentageApm >= minPercentageHealthyApms
    return NetworkMinerMonitorResponse(
        isVpmHealthy = if (totalVpms > 0) isVpmHealthy else null,
        isApmHealthy = if (totalApms > 0) isApmHealthy else null,
        monitors = this
    )
}

private fun List<AbfiMonitorResponse>.toNetworkAbfiMonitorResponse(
    minPercentageHealthy: Int,
    total: Int
): NetworkAbfiMonitorResponse {
    val healthyCount = count { it.isHealthy.isHealthy }
    val percentage = getHealthyPercentage(
        count = healthyCount,
        totalCount = total
    )
    val isHealthy = percentage >= minPercentageHealthy
    return NetworkAbfiMonitorResponse(
        isHealthy = isHealthy,
        monitors = this
    )
}

private fun List<VbfiMonitorResponse>.toNetworkVbfiMonitorResponse(
    minPercentageHealthy: Int,
    total: Int
): NetworkVbfiMonitorResponse {
    val healthyCount = count { it.isHealthy.isHealthy }
    val percentage = getHealthyPercentage(
        count = healthyCount,
        totalCount = total
    )
    val isHealthy = percentage >= minPercentageHealthy
    return NetworkVbfiMonitorResponse(
        isHealthy = isHealthy,
        monitors = this
    )
}

private fun List<PopSubsidiesMonitorResponse>.toNetworkPopSubsidiesMonitorResponse(
    minPercentageHealthy: Int,
    total: Int
): NetworkPopSubsidiesMonitorResponse  {
    val healthyCount = count { it.isHealthy.isHealthy }
    val percentage = getHealthyPercentage(
        count = healthyCount,
        totalCount = total
    )
    val isHealthy = percentage >= minPercentageHealthy
    return NetworkPopSubsidiesMonitorResponse(
        isHealthy = isHealthy,
        monitors = this
    )
}

private fun List<NodeCoreMonitorResponse>.toNetworkNodeCoreMonitorResponse(
    minPercentageHealthy: Int,
    total: Int
): NetworkNodeCoreMonitorResponse  {
    val healthyCount = count { it.isHealthy.isHealthy }
    val percentage = getHealthyPercentage(
        count = healthyCount,
        totalCount = total
    )
    val isHealthy = percentage >= minPercentageHealthy
    return NetworkNodeCoreMonitorResponse(
        isHealthy = isHealthy,
        monitors = this
    )
}

private fun List<ExplorerMonitorResponse>.toNetworkExplorerMonitorResponse(
    minPercentageHealthy: Int,
    total: Int
): NetworkExplorerMonitorResponse {
    val healthyCount = count { it.isHealthy.isHealthy }
    val percentage = getHealthyPercentage(
        count = healthyCount,
        totalCount = total
    )
    val isHealthy = percentage >= minPercentageHealthy
    return NetworkExplorerMonitorResponse(
        isHealthy = isHealthy,
        monitors = this
    )
}