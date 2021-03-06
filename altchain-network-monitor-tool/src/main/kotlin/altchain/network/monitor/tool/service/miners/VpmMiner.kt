package altchain.network.monitor.tool.service.miners

import altchain.network.monitor.tool.IMPORTANT_METRICS
import altchain.network.monitor.tool.MINER_METRICS
import altchain.network.monitor.tool.MetricType
import altchain.network.monitor.tool.MinerConfig
import altchain.network.monitor.tool.persistence.repositories.MinerMonitorRepository
import altchain.network.monitor.tool.persistence.tables.MetricRecord
import altchain.network.monitor.tool.persistence.tables.MetricsRecord
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

class VpmMiner(
    private val minerMonitorRepository: MinerMonitorRepository
) : Miner {
    override val type: MinerType = MinerType.VPM

    private val httpClients: MutableMap<String, HttpClient> by lazy {
        HashMap()
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun getMonitor(networkId: String, id: String, config: MinerConfig): MinerMonitor {
        httpClients.getOrPut("$networkId/$id") { createHttpClient(config.auth).also {
            logger.info { "($networkId/$id) Creating http client..." }
        } }.also { httpClient ->
            val now = now()
            val version: String = try {
                httpClient.get<VersionResponse>("${config.apiUrl}/api/version").name
            } catch (exception: Exception) {
                "UNKNOWN"
            }
            val metrics: String = httpClient.get("${config.apiUrl}/metrics")
            val debugInformation: DiagnosticInformation = httpClient.get("${config.apiUrl}/api/debug")

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
                type = config.type,
                id = id,
                startDate = startDate,
                limit = config.compareLatestRecordCount
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
                version = version,
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

fun MetricsRecord.extractMetricIntValue(type: MetricType): Int {
    return try {
        metrics.find {
            it.type == type
        }?.value?.toDouble()?.toInt() ?: 0
    } catch(exception: NumberFormatException) {
        0
    }
}

fun String.extractValuesFromMetrics(metrics: Map<MetricType, String>): MetricsRecord {
    val lines = lines().filter { line ->
        !line.startsWith("#")
    }
    val metricsRecord = metrics.entries.asSequence().mapNotNull { (type, field) ->
        lines.find { line -> line.contains(field) }?.let {
            MetricRecord(type, it.replace(field, "").trim())
        }
    }.toSet()
    return MetricsRecord(metricsRecord)
}