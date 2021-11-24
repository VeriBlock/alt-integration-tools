package altchain.network.monitor.tool.api.controller

import altchain.network.monitor.tool.persistence.tables.MinerType
import com.papsign.ktor.openapigen.annotations.Response
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Response
@Serializable
data class NetworkMonitorSummaryResponse(
    val networks: List<NetworkMonitorResponse>
)

@Response
@Serializable
data class NetworkMonitorResponse(
    val networkId: String,
    val isHealthy: HealthyStatusReportResponse,
    val networkNodeCoreMonitor: NetworkNodeCoreMonitorResponse?,
    val networkAltDaemonMonitor: NetworkAltDaemonMonitorResponse?,
    val networkExplorerMonitor: NetworkExplorerMonitorResponse?,
    val networkAbfiMonitor: NetworkAbfiMonitorResponse?,
    val networkVbfiMonitor: NetworkVbfiMonitorResponse?,
    val networkMinerMonitor: NetworkMinerMonitorResponse?
)

@Response
@Serializable
data class NetworkMinerMonitorResponse(
    val isVpmHealthy: Boolean? = null,
    val isApmHealthy: Boolean? = null,
    val minerMonitors: List<MinerMonitorResponse>
)

@Response
@Serializable
data class MinerMonitorResponse(
    val networkId: String,
    val minerId: String,
    val minerVersion: String = "",
    val host: String,
    val minerType: MinerType,
    val startedOperationCount: Int = 0,
    val completedOperationCount: Int = 0,
    val failedOperationCount: Int = 0,
    val isMining: Boolean = false,
    val isHealthyByTime: Boolean = false,
    val isHealthyByOperations: Boolean = false,
    val isHealthy: HealthyStatusResponse,
    val metrics: List<MetricResponse> = emptyList(),
    val uptimeSeconds: Int = 0,
    val addedAt: Instant = Instant.DISTANT_PAST
)

@Response
@Serializable
data class MetricResponse(
    val type: String,
    val value: String
)

@Response
@Serializable
data class NetworkNodeCoreMonitorResponse(
    val isHealthy: Boolean,
    val nodeCoreMonitors: List<NodeCoreMonitorResponse>
)

@Response
@Serializable
data class NodeCoreMonitorResponse(
    val networkId: String,
    val nodecoreId: String,
    val nodecoreVersion: String = "",
    val host: String,
    val localHeight: Int = 0,
    val networkHeight: Int = 0,
    val blockDifference: Int = 0,
    val isHealthyByBlocks: Boolean = false,
    val isHealthyByTime: Boolean = false,
    val isHealthy: HealthyStatusResponse,
    val addedAt: Instant = Instant.DISTANT_PAST
)

@Response
@Serializable
data class HealthyStatusResponse(
    val isHealthy: Boolean,
    val reason: String? = null
)

@Response
@Serializable
data class HealthyStatusReportResponse(
    val isHealthy: Boolean,
    val diagnostics: List<String>? = null
)

@Response
@Serializable
data class NetworkAltDaemonMonitorResponse(
    val isHealthy: Boolean,
    val altDaemonMonitors: List<AltDaemonMonitorResponse>
)

@Response
@Serializable
data class AltDaemonMonitorResponse(
    val networkId: String,
    val altDaemonId: String,
    val host: String,
    val localHeight: Int = 0,
    val networkHeight: Int = 0,
    val blockDifference: Int = 0,
    val isHealthyByBlocks: Boolean = false,
    val isHealthyByTime: Boolean = false,
    val isHealthy: HealthyStatusResponse,
    val addedAt: Instant = Instant.DISTANT_PAST
)

@Response
@Serializable
data class NetworkExplorerMonitorResponse(
    val isHealthy: Boolean,
    val explorerMonitors: List<ExplorerMonitorResponse>
)

@Response
@Serializable
data class ExplorerMonitorResponse(
    val networkId: String,
    val explorerId: String,
    val host: String,
    val blockCount: Int = 0,
    val atvCount: Int = 0,
    val vtbCount: Int = 0,
    val vbkCount: Int = 0,
    val atvBlocks: Set<Int> = emptySet(),
    val vtbBlocks: Set<Int> = emptySet(),
    val vbkBlocks: Set<Int> = emptySet(),
    val isHealthyByBlocks: Boolean = false,
    val isHealthyByTime: Boolean = false,
    val isHealthy: HealthyStatusResponse,
    val addedAt: Instant = Instant.DISTANT_PAST
)

@Response
@Serializable
data class NetworkAbfiMonitorResponse(
    val isHealthy: Boolean,
    val abfiMonitors: List<AbfiMonitorResponse>
)

@Response
@Serializable
data class AbfiMonitorResponse(
    val networkId: String,
    val abfiId: String,
    val version: String = "",
    val host: String,
    val prefix: String,
    val blockInfo: AbfiBlockSummaryResponse? = null,
    val lastFinalizedBlockHeight: Int = 0,
    val lastNetworkBlockHeight: Int = 0,
    val blockDifference: Int = 0,
    val isHealthyByTime: Boolean = false,
    val isHealthyByLastFinalizedBlockBtc: Boolean = false,
    val isHealthyByBlocks: Boolean = false,
    val isHealthy: HealthyStatusResponse,
    val addedAt: Instant = Instant.DISTANT_PAST
)

@Response
@Serializable
data class AbfiBlockSummaryResponse(
    val blocks: List<AbfiBlockInfoSummaryResponse>
)

@Response
@Serializable
data class AbfiBlockInfoSummaryResponse(
    val name: String,
    val blockInfo: AbfiBlockInfoResponse
)

@Response
@Serializable
data class AbfiBlockInfoResponse(
    val height: Int?,
    val spFinality: Int?,
    val bitcoinFinality: Int?,
    val endorsedInHeight: Int?,
    val verifiedInHeight: Int?
)

@Response
@Serializable
data class NetworkVbfiMonitorResponse(
    val isHealthy: Boolean,
    val vbfiMonitors: List<VbfiMonitorResponse>
)

@Response
@Serializable
data class VbfiMonitorResponse(
    val networkId: String,
    val vbfiId: String,
    val version: String = "",
    val host: String,
    val lastBlockHeight: Int = 0,
    val lastBlockFinalizedBtcHeight: Int = 0,
    val lastExplorerBlockHeight: Int = 0,
    val lastBlockDifference: Int = 0,
    val lastBlockFinalizedBtcDifference: Int = 0,
    val isHealthyByTime: Boolean = false,
    val isHealthyByLastBlock: Boolean = false,
    val isHealthyByLastFinalizedBlockBtc: Boolean = false,
    val isHealthy: HealthyStatusResponse,
    val addedAt: Instant = Instant.DISTANT_PAST
)