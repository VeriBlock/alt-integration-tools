package altchain.network.monitor.tool.service.miners

import altchain.network.monitor.tool.IMPORTANT_METRICS
import altchain.network.monitor.tool.MINER_METRICS
import altchain.network.monitor.tool.MetricType
import altchain.network.monitor.tool.MinerConfig
import altchain.network.monitor.tool.persistence.repositories.MinerMonitorRepository
import altchain.network.monitor.tool.persistence.tables.MinerMonitor
import altchain.network.monitor.tool.persistence.tables.MinerType
import altchain.network.monitor.tool.util.createHttpClient
import altchain.network.monitor.tool.util.createLogger
import altchain.network.monitor.tool.util.now
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

private val logger = createLogger {}

class ApmMiner(
    private val minerMonitorRepository: MinerMonitorRepository
) : Miner {
    override val type: MinerType = MinerType.APM

    private val httpClients: MutableMap<String, HttpClient> by lazy {
        HashMap()
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun getMinerMonitor(networkId: String, minerId: String, minerConfig: MinerConfig): MinerMonitor {
        httpClients.getOrPut("$networkId/$minerId") { createHttpClient(minerConfig.auth).also {
            logger.info { "($networkId/$minerId) Creating http client..." }
        } }.also { httpClient ->
            val now = now()
            val version: String = try {
                httpClient.get<VersionResponse>("${minerConfig.apiUrl}/api/version").name
            } catch (exception: Exception) {
                "UNKNOWN"
            }
            val metrics: String = httpClient.get("${minerConfig.apiUrl}/metrics")
            val debugInformation: DiagnosticInformation = httpClient.get("${minerConfig.apiUrl}/api/debug")

            val generalMetrics = metrics.extractValuesFromMetrics(IMPORTANT_METRICS)
            val minerMetrics = metrics.extractValuesFromMetrics(MINER_METRICS)
            val minerDiagnostic = debugInformation.information.asSequence().filter {
                it.startsWith("FAIL")
            }.toSet()

            val startedOperationCount = minerMetrics.extractMetricIntValue(MetricType.STARTED_OPERATIONS)
            val completedOperationCount = minerMetrics.extractMetricIntValue(MetricType.COMPLETED_OPERATIONS)
            val failedOperationCount = minerMetrics.extractMetricIntValue(MetricType.FAILED_OPERATIONS)
            val uptimeSeconds = minerMetrics.extractMetricIntValue(MetricType.UPTIME_SECONDS)

            val startDate = now().minus(Duration.seconds(uptimeSeconds))
            val latestRecords = minerMonitorRepository.findLatests(
                networkId = networkId,
                type = minerConfig.type,
                minerId = minerId,
                startDate = startDate,
                limit = minerConfig.compareLatestRecordCount
            )

            val isMining = if (latestRecords.isEmpty()) {
                startedOperationCount > 0
            } else {
                val minStartedOperationCount = latestRecords.minOf { it.startedOperationCount }
                val maxStartedOperationCount = latestRecords.maxOf { it.startedOperationCount }
                latestRecords.any {
                    startedOperationCount > it.startedOperationCount
                } || maxStartedOperationCount > minStartedOperationCount
            }

            return MinerMonitor(
                minerVersion = version,
                startedOperationCount = startedOperationCount,
                completedOperationCount = completedOperationCount,
                failedOperationCount = failedOperationCount,
                isMining = isMining,
                minerDiagnostic = minerDiagnostic,
                metrics = generalMetrics,
                uptimeSeconds = uptimeSeconds,
                addedAt = now
            )
        }
    }
}

