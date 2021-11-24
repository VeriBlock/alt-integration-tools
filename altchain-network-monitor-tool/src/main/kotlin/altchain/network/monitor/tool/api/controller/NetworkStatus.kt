package altchain.network.monitor.tool.api.controller

import altchain.network.monitor.tool.NetworkConfig
import altchain.network.monitor.tool.api.BadRequestException
import altchain.network.monitor.tool.persistence.repositories.AbfiMonitorRepository
import altchain.network.monitor.tool.persistence.repositories.AltDaemonMonitorRepository
import altchain.network.monitor.tool.persistence.repositories.ExplorerMonitorRepository
import altchain.network.monitor.tool.persistence.repositories.MinerMonitorRepository
import altchain.network.monitor.tool.persistence.repositories.NodeCoreMonitorRepository
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
    private val minerMonitorRepository: MinerMonitorRepository
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
            val nodecoreMonitors = nodeCoreMonitorRepository.find(
                networkId = networkId,
                nodecoreIds = network.nodecores.keys.toLowerCase()
            )
            val nodeCoreMonitorResponse = nodecoreMonitors.map { nodeCoreMonitorRecord ->
                nodeCoreMonitorRecord.toNodeCoreMonitorResponse(network.maxHealthyByTime)
            }
            val nodeCoreMonitorNotPresentResponse = network.nodecores.filterNot { entry ->
                nodecoreMonitors.any { monitor ->
                    monitor.nodecoreId == entry.key
                }
            }.map {
                notPresentNodeCoreMonitorResponse(
                    networkId = networkId,
                    nodecoreId = it.key,
                    host = it.value.host
                )
            }
            (nodeCoreMonitorResponse + nodeCoreMonitorNotPresentResponse).toNetworkNodeCoreMonitorResponse(
                minPercentageHealthyNodeCores = network.minPercentageHealthyNodeCores,
                totalNodeCores = network.nodecores.size
            )
        } else {
            null
        }
        val networkAltDaemonMonitorResponse = if (network.altDaemons.isNotEmpty()) {
            val altDaemonMonitors = altDaemonMonitorRepository.find(
                networkId = networkId,
                altDaemonIds = network.altDaemons.keys.toLowerCase()
            )
            val altDaemonMonitorResponse = altDaemonMonitors.map { altDaemonMonitorRecord ->
                altDaemonMonitorRecord.toAltDaemonMonitorResponse(network.maxHealthyByTime)
            }
            val altDaemonMonitorNotPresentResponse = network.altDaemons.filterNot { entry ->
                altDaemonMonitors.any { monitor ->
                    monitor.altDaemonId == entry.key
                }
            }.map {
                notPresentAltDaemonMonitorResponse(
                    networkId = networkId,
                    altDaemonId = it.key,
                    host = altchainService.getPluginByKey(it.key)?.config?.host ?: "Not Found"
                )
            }
            (altDaemonMonitorNotPresentResponse + altDaemonMonitorResponse).toNetworkAltDaemonMonitorResponse(
                minPercentageHealthyAltDaemons = network.minPercentageHealthyAltDaemons,
                totalAltDaemons = network.altDaemons.size
            )
        } else {
            null
        }
        val networkExplorerMonitorResponse = if (network.explorers.isNotEmpty()) {
            val explorerMonitors = explorerMonitorRepository.find(
                networkId = networkId,
                explorerIds = network.explorers.keys.toLowerCase()
            )
            val explorerMonitorResponse = explorerMonitors.map { explorerMonitorRecord ->
                explorerMonitorRecord.toExplorerMonitorResponse(network.maxHealthyByTime)
            }
            val explorerNotPresentResponse = network.explorers.filterNot { entry ->
                explorerMonitors.any { monitor ->
                    monitor.explorerId == entry.key
                }
            }.map {
                notPresentExplorerMonitorResponse(
                    networkId = networkId,
                    explorerId = it.key,
                    host = it.value.url
                )
            }
            (explorerNotPresentResponse + explorerMonitorResponse).toNetworkExplorerMonitorResponse(
                minPercentageHealthyExplorers = network.minPercentageHealthyExplorers,
                totalExplorers = network.explorers.size
            )
        } else {
            null
        }

        val networkAbfiMonitorResponse = if (network.abfis.isNotEmpty()) {
            val abfiMonitors = abfiMonitorRepository.find(
                networkId = networkId,
                abfiIds = network.abfis.keys.toLowerCase()
            )
            val abfiMonitorResponse = abfiMonitors.map { abfiMonitorRecord ->
                abfiMonitorRecord.toAbfiMonitorResponse(network.maxHealthyByTime)
            }
            val abfiNotPresentResponse = network.abfis.filterNot { entry ->
                abfiMonitors.any { monitor ->
                    monitor.abfiId == entry.key
                }
            }.map {
                notPresentAbfiMonitorResponse(
                    networkId = networkId,
                    abfiId = it.key,
                    host = it.value.apiUrl,
                    prefix = it.value.prefix
                )
            }
            (abfiNotPresentResponse + abfiMonitorResponse).toNetworkAbfiMonitorResponse(
                minPercentageHealthyAbfis = network.minPercentageHealthyAbfis,
                totalAbfis = network.abfis.size
            )
        } else {
            null
        }

        val networkVbfiMonitorResponse = if (network.vbfis.isNotEmpty()) {
            val vbfiMonitors = vbfiMonitorRepository.find(
                networkId = networkId,
                vbfiIds = network.vbfis.keys.toLowerCase()
            )
            val vbfiMonitorResponse = vbfiMonitors.map { vbfiMonitorRecord ->
                vbfiMonitorRecord.toVbfiMonitorResponse(network.maxHealthyByTime)
            }
            val vbfiNotPresentResponse = network.vbfis.filterNot { entry ->
                vbfiMonitors.any { monitor ->
                    monitor.vbfiId == entry.key
                }
            }.map {
                notPresentVbfiMonitorResponse(
                    networkId = networkId,
                    vbfiId = it.key,
                    host = it.value.apiUrl
                )
            }
            (vbfiNotPresentResponse + vbfiMonitorResponse).toNetworkVbfiMonitorResponse(
                minPercentageHealthyVbfis = network.minPercentageHealthyVbfis,
                totalVbfis = network.vbfis.size
            )
        } else {
            null
        }

        val networkMinerMonitorResponse = if (network.miners.isNotEmpty()) {
            val minerMonitors = minerMonitorRepository.find(
                networkId = networkId,
                minerIds = network.miners.keys.toLowerCase()
            )
            val minerMonitorResponse = minerMonitors.map { minerMonitorRecord ->
                minerMonitorRecord.toMinerMonitorResponse(
                    maxHealthyByTime = network.maxHealthyByTime,
                    operationsThreshold = if (minerMonitorRecord.minerType == MinerType.VPM) {
                        network.maxPercentageNotHealthyVpmOperations
                    } else {
                        network.maxPercentageNotHealthyApmOperations
                    }
                )
            }
            val minerNotPresentResponse = network.miners.filterNot { entry ->
                minerMonitors.any { monitor ->
                    monitor.minerId == entry.key
                }
            }.map {
                notPresentMinerMonitorResponse(
                    networkId = networkId,
                    minerId = it.key,
                    host = it.value.apiUrl,
                    minerType = it.value.type
                )
            }
            (minerNotPresentResponse + minerMonitorResponse).toNetworkMinerMonitorResponse(
                minPercentageHealthyVpms = network.minPercentageHealthyVpms,
                minPercentageHealthyApms = network.minPercentageHealthyApms,
                totalVpms = network.miners.values.count { it.type == MinerType.VPM },
                totalApms = network.miners.values.count { it.type == MinerType.APM }
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
                networkMinerMonitorResponse?.isApmHealthy ?: true


        val nodeCoreDiagnostics = networkNodeCoreMonitorResponse?.nodeCoreMonitors?.filter {
            !it.isHealthy.isHealthy
        }?.mapNotNull { nodeCoreMonitorResponse ->
            nodeCoreMonitorResponse.isHealthy.reason
        } ?: emptyList()

        val altDaemonDiagnostics = networkAltDaemonMonitorResponse?.altDaemonMonitors?.filter {
            !it.isHealthy.isHealthy
        }?.mapNotNull { altDaemonMonitorResponse ->
            altDaemonMonitorResponse.isHealthy.reason
        } ?: emptyList()

        val explorerDiagnostics = networkExplorerMonitorResponse?.explorerMonitors?.filter {
            !it.isHealthy.isHealthy
        }?.mapNotNull { explorerMonitorResponse ->
            explorerMonitorResponse.isHealthy.reason
        } ?: emptyList()

        val abfiDiagnostics = networkAbfiMonitorResponse?.abfiMonitors?.filter {
            !it.isHealthy.isHealthy
        }?.mapNotNull { abfiMonitorResponse ->
            abfiMonitorResponse.isHealthy.reason
        } ?: emptyList()

        val vbfiDiagnostics = networkVbfiMonitorResponse?.vbfiMonitors?.filter {
            !it.isHealthy.isHealthy
        }?.mapNotNull { vbfiMonitorResponse ->
            vbfiMonitorResponse.isHealthy.reason
        } ?: emptyList()

        val minerDiagnostics = networkMinerMonitorResponse?.minerMonitors?.filter {
            !it.isHealthy.isHealthy
        }?.mapNotNull { minerMonitorResponse ->
            minerMonitorResponse.isHealthy.reason
        } ?: emptyList()

        val response = NetworkMonitorResponse(
            networkId = networkId,
            isHealthy = HealthyStatusReportResponse(
                isHealthy = isHealthy,
                diagnostics = (nodeCoreDiagnostics + altDaemonDiagnostics + explorerDiagnostics + abfiDiagnostics + minerDiagnostics + vbfiDiagnostics).ifEmpty {
                    null
                }
            ),
            networkNodeCoreMonitor = networkNodeCoreMonitorResponse,
            networkAltDaemonMonitor = networkAltDaemonMonitorResponse,
            networkExplorerMonitor = networkExplorerMonitorResponse,
            networkAbfiMonitor = networkAbfiMonitorResponse,
            networkVbfiMonitor = networkVbfiMonitorResponse,
            networkMinerMonitor = networkMinerMonitorResponse
        )
        return response
    }
}

fun notPresentMinerMonitorResponse(
    networkId: String,
    minerId: String,
    host: String,
    minerType: MinerType
): MinerMonitorResponse {
    return MinerMonitorResponse(
        networkId = networkId,
        minerId = minerId,
        host = host,
        minerType = minerType,
        isHealthy = HealthyStatusResponse(
            isHealthy = false,
            reason = defaultDownMessage.replace("\$id", minerId)
        )
    )
}

fun MinerMonitorRecord.toMinerMonitorResponse(
    maxHealthyByTime: Int,
    operationsThreshold: Int
) : MinerMonitorResponse {
    val timeDifference = Duration.between(addedAt.toJavaInstant(), now().toJavaInstant()).toMinutes()
    val isHealthyByTime = timeDifference <= maxHealthyByTime
    val percentage = getHealthyPercentage(
        count = failedOperationCount,
        totalCount = startedOperationCount
    )
    val isHealthyByOperations = percentage < operationsThreshold
    val isHealthyByOperationsReport = if (!isHealthyByOperations) {
        "$minerId operations are failing beyond the threshold ($operationsThreshold%), started operations: $startedOperationCount, completed operations: $completedOperationCount, failed operations: $failedOperationCount"
    } else {
        null
    }
    val isHealthyByTimeReport = if (!isHealthyByTime) {
        defaultOldDataMessage.replace("\$id", minerId).replace("\$timeDifference", "$timeDifference")
    } else {
        null
    }

    val isHealthyByMining = if (!isMining) {
        "$minerId is not creating new operations"
    } else {
        null
    }

    return MinerMonitorResponse(
        networkId = networkId,
        minerId = minerId,
        minerVersion = minerVersion,
        host = host,
        minerType = minerType,
        startedOperationCount = startedOperationCount,
        completedOperationCount = completedOperationCount,
        failedOperationCount = failedOperationCount,
        isMining = isMining,
        isHealthyByTime = isHealthyByTime,
        isHealthyByOperations = isHealthyByOperations,
        isHealthy = HealthyStatusResponse(
            isHealthy = isHealthyByTime && isHealthyByOperations && isMining,
            reason = listOfNotNull(isHealthyByTimeReport, isHealthyByMining, isHealthyByOperationsReport).firstOrNull()
        ),
        metrics = metrics.metrics.map {
            it.toMetricResponse()
        },
        uptimeSeconds = uptimeSeconds,
        addedAt = addedAt
    )
}

fun List<MinerMonitorResponse>.toNetworkMinerMonitorResponse(
    minPercentageHealthyVpms: Int,
    minPercentageHealthyApms: Int,
    totalVpms: Int,
    totalApms: Int
): NetworkMinerMonitorResponse {
    val healthyVpmCount = asSequence().filter {
        it.minerType == MinerType.VPM
    }.count {
        it.isHealthy.isHealthy
    }
    val percentageVpm = getHealthyPercentage(
        count = healthyVpmCount,
        totalCount = totalVpms
    )
    val isVpmHealthy = percentageVpm >= minPercentageHealthyVpms
    val healthyApmCount = asSequence().filter {
        it.minerType == MinerType.APM
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
        minerMonitors = this
    )
}

fun List<AbfiMonitorResponse>.toNetworkAbfiMonitorResponse(
    minPercentageHealthyAbfis: Int,
    totalAbfis: Int
): NetworkAbfiMonitorResponse {
    val healthyCount = count { it.isHealthy.isHealthy }
    val percentage = getHealthyPercentage(
        count = healthyCount,
        totalCount = totalAbfis
    )
    val isHealthy = percentage >= minPercentageHealthyAbfis
    return NetworkAbfiMonitorResponse(
        isHealthy = isHealthy,
        abfiMonitors = this
    )
}

fun notPresentAbfiMonitorResponse(
    networkId: String,
    abfiId: String,
    host: String,
    prefix: String,
): AbfiMonitorResponse {
    return AbfiMonitorResponse(
        networkId = networkId,
        abfiId = abfiId,
        host = host,
        prefix = prefix,
        isHealthy = HealthyStatusResponse(
            isHealthy = false,
            reason = defaultDownMessage.replace("\$id", abfiId)
        )
    )
}

fun List<VbfiMonitorResponse>.toNetworkVbfiMonitorResponse(
    minPercentageHealthyVbfis: Int,
    totalVbfis: Int
): NetworkVbfiMonitorResponse {
    val healthyCount = count { it.isHealthy.isHealthy }
    val percentage = getHealthyPercentage(
        count = healthyCount,
        totalCount = totalVbfis
    )
    val isHealthy = percentage >= minPercentageHealthyVbfis
    return NetworkVbfiMonitorResponse(
        isHealthy = isHealthy,
        vbfiMonitors = this
    )
}

fun notPresentVbfiMonitorResponse(
    networkId: String,
    vbfiId: String,
    host: String
): VbfiMonitorResponse {
    return VbfiMonitorResponse(
        networkId = networkId,
        vbfiId = vbfiId,
        host = host,
        isHealthy = HealthyStatusResponse(
            isHealthy = false,
            reason = defaultDownMessage.replace("\$id", vbfiId)
        )
    )
}

fun AbfiBlockRecord.toAbfiBlockInfoSummaryResponse(): AbfiBlockInfoSummaryResponse {
    return AbfiBlockInfoSummaryResponse(
       name = name,
       blockInfo =  blockInfo.toAbfiBlockInfoResponse()
    )
}

fun AbfiBlockInfoRecord.toAbfiBlockInfoResponse(): AbfiBlockInfoResponse {
    return AbfiBlockInfoResponse(
        height = height,
        spFinality = spFinality,
        bitcoinFinality = bitcoinFinality,
        endorsedInHeight = endorsedInHeight,
        verifiedInHeight = verifiedInHeight
    )
}

fun AbfiMonitorRecord.toAbfiMonitorResponse(
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
        abfiId = abfiId,
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


fun VbfiMonitorRecord.toVbfiMonitorResponse(
    maxHealthyByTime: Int
): VbfiMonitorResponse {
    val timeDifference = Duration.between(addedAt.toJavaInstant(), now().toJavaInstant()).toMinutes()
    val isHealthyByTime = timeDifference <= maxHealthyByTime

    val lastBlockDifference = abs(lastExplorerBlockHeight - lastBlockHeight)
    val isHealthyByLastBlockReport = if (!isLastBlockSynchronized) {
        defaultNotSyncNodeMessage.replace("\$id", vbfiId).replace("\$blockDifference", "$lastBlockDifference")
            .replace("\$localHeight", "$lastBlockHeight").replace("\$networkHeight", "$lastExplorerBlockHeight")
    } else {
        null
    }

    val lastBlockFinalizedBtcDifference = abs(lastExplorerBlockHeight - lastBlockFinalizedBtcHeight)
    val isHealthyByLastBlockFinalizedReport = if (!isLastBlockFinalizedBtcSynchronized) {
        defaultNotSyncNodeMessage.replace("\$id", vbfiId).replace("\$blockDifference", "$lastBlockFinalizedBtcDifference")
            .replace("\$localHeight", "$lastBlockFinalizedBtcHeight").replace("\$networkHeight", "$lastExplorerBlockHeight")
    } else {
        null
    }

    val isHealthyByTimeReport = if (!isHealthyByTime) {
        defaultOldDataMessage.replace("\$id", vbfiId).replace("\$timeDifference", "$timeDifference")
    } else {
        null
    }
    return VbfiMonitorResponse(
        networkId = networkId,
        vbfiId = vbfiId,
        version = vbfiVersion,
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


fun notPresentNodeCoreMonitorResponse(
    networkId: String,
    nodecoreId: String,
    host: String,
): NodeCoreMonitorResponse {
    return NodeCoreMonitorResponse(
        networkId = networkId,
        nodecoreId = nodecoreId,
        host = host,
        isHealthy = HealthyStatusResponse(
            isHealthy = false,
            reason = defaultDownMessage.replace("\$id", nodecoreId)
        )
    )
}

fun notPresentAltDaemonMonitorResponse(
    networkId: String,
    altDaemonId: String,
    host: String,
): AltDaemonMonitorResponse {
    return AltDaemonMonitorResponse(
        networkId = networkId,
        altDaemonId = altDaemonId,
        host = host,
        isHealthy = HealthyStatusResponse(
            isHealthy = false,
            reason = defaultDownMessage.replace("\$id", altDaemonId)
        ),
        addedAt = Instant.DISTANT_PAST
    )
}

fun NodeCoreMonitorRecord.toNodeCoreMonitorResponse(
    maxHealthyByTime: Int
): NodeCoreMonitorResponse {
    val timeDifference = Duration.between(addedAt.toJavaInstant(), now().toJavaInstant()).toMinutes()
    val isHealthyByTime = timeDifference <= maxHealthyByTime
    val blockDifference = abs(localHeight - networkHeight)
    val isHealthyByBlocksReport = if (!isSynchronized) {
        defaultNotSyncNodeMessage.replace("\$id", nodecoreId).replace("\$blockDifference", "$blockDifference")
            .replace("\$localHeight", "$localHeight").replace("\$networkHeight", "$networkHeight")
    } else {
        null
    }
    val isHealthyByTimeReport = if (!isHealthyByTime) {
        defaultOldDataMessage.replace("\$id", nodecoreId).replace("\$timeDifference", "$timeDifference")
    } else {
        null
    }
    return NodeCoreMonitorResponse(
        networkId = networkId,
        nodecoreId = nodecoreId,
        nodecoreVersion = nodecoreVersion,
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

fun List<AltDaemonMonitorResponse>.toNetworkAltDaemonMonitorResponse(
    minPercentageHealthyAltDaemons: Int,
    totalAltDaemons: Int
): NetworkAltDaemonMonitorResponse  {
    val healthyCount = count { it.isHealthy.isHealthy }
    val percentage = getHealthyPercentage(
        count = healthyCount,
        totalCount = totalAltDaemons
    )
    val isHealthy = percentage >= minPercentageHealthyAltDaemons
    return NetworkAltDaemonMonitorResponse(
        isHealthy = isHealthy,
        altDaemonMonitors = this
    )
}

fun List<NodeCoreMonitorResponse>.toNetworkNodeCoreMonitorResponse(
    minPercentageHealthyNodeCores: Int,
    totalNodeCores: Int
): NetworkNodeCoreMonitorResponse  {
    val healthyCount = count { it.isHealthy.isHealthy }
    val percentage = getHealthyPercentage(
        count = healthyCount,
        totalCount = totalNodeCores
    )
    val isHealthy = percentage >= minPercentageHealthyNodeCores
    return NetworkNodeCoreMonitorResponse(
        isHealthy = isHealthy,
        nodeCoreMonitors = this
    )
}

fun AltDaemonMonitorRecord.toAltDaemonMonitorResponse(
    maxHealthyByTime: Int
): AltDaemonMonitorResponse {
    val timeDifference = Duration.between(addedAt.toJavaInstant(), now().toJavaInstant()).toMinutes()
    val isHealthyByTime = timeDifference <= maxHealthyByTime
    val blockDifference = abs(localHeight - networkHeight)
    val isHealthyByBlocksReport = if (!isSynchronized) {
        defaultNotSyncNodeMessage.replace("\$id", altDaemonId).replace("\$blockDifference", "$blockDifference")
            .replace("\$localHeight", "$localHeight").replace("\$networkHeight", "$networkHeight")
    } else {
        null
    }
    val isHealthyByTimeReport = if (!isHealthyByTime) {
        defaultOldDataMessage.replace("\$id", altDaemonId).replace("\$timeDifference", "$timeDifference")
    } else {
        null
    }
    return AltDaemonMonitorResponse(
        networkId = networkId,
        altDaemonId = altDaemonId,
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

fun List<ExplorerMonitorResponse>.toNetworkExplorerMonitorResponse(
    minPercentageHealthyExplorers: Int,
    totalExplorers: Int
): NetworkExplorerMonitorResponse {
    val healthyCount = count { it.isHealthy.isHealthy }
    val percentage = getHealthyPercentage(
        count = healthyCount,
        totalCount = totalExplorers
    )
    val isHealthy = percentage >= minPercentageHealthyExplorers
    return NetworkExplorerMonitorResponse(
        isHealthy = isHealthy,
        explorerMonitors = this
    )
}

fun notPresentExplorerMonitorResponse(
    networkId: String,
    explorerId: String,
    host: String
): ExplorerMonitorResponse {
    return ExplorerMonitorResponse(
        networkId = networkId,
        explorerId = explorerId,
        host = host,
        isHealthy = HealthyStatusResponse(
            isHealthy = false,
            reason = defaultDownMessage.replace("\$id", explorerId)
        )
    )
}

fun ExplorerMonitorRecord.toExplorerMonitorResponse(
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
        "$explorerId doesn't have any ${blocks.filter { it.isNotEmpty() }.joinToString()}"
    } else {
        null
    }
    val isHealthyByTimeReport = if (!isHealthyByTime) {
        defaultOldDataMessage.replace("\$id", explorerId).replace("\$timeDifference", "$timeDifference")
    } else {
        null
    }
    return ExplorerMonitorResponse(
        networkId = networkId,
        explorerId = explorerId,
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