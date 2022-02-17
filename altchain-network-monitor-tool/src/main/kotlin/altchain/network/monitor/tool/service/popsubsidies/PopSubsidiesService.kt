package altchain.network.monitor.tool.service.popsubsidies

import altchain.network.monitor.tool.MetricType
import altchain.network.monitor.tool.POP_SUBSIDIES_METRICS
import altchain.network.monitor.tool.PopSubsidiesConfig
import altchain.network.monitor.tool.persistence.tables.PopSubsidiesMonitor
import altchain.network.monitor.tool.service.miners.extractMetricIntValue
import altchain.network.monitor.tool.service.miners.extractValuesFromMetrics
import altchain.network.monitor.tool.util.createHttpClient
import altchain.network.monitor.tool.util.createLogger
import altchain.network.monitor.tool.util.now
import io.ktor.client.HttpClient
import io.ktor.client.request.get

private val logger = createLogger {}

class PopSubsidiesService {
    private val httpClients: MutableMap<String, HttpClient> by lazy {
        HashMap()
    }

    suspend fun getMonitor(networkId: String, id: String, config: PopSubsidiesConfig): PopSubsidiesMonitor {
        httpClients.getOrPut("$networkId/$id") { createHttpClient(config.auth).also {
            logger.info { "($networkId/$id) Creating http client..." }
        } }.also { httpClient ->
            val metrics: String = httpClient.get<String>("${config.apiUrl}/metrics")
            val metricsRecord = metrics.extractValuesFromMetrics(POP_SUBSIDIES_METRICS)
            val startedOperationCount = metricsRecord.extractMetricIntValue(MetricType.STARTED_OPERATIONS)
            val completedOperationCount = metricsRecord.extractMetricIntValue(MetricType.COMPLETED_OPERATIONS)
            val failedOperationCount = metricsRecord.extractMetricIntValue(MetricType.FAILED_OPERATIONS)
            return PopSubsidiesMonitor(
                version = "UNKNOWN",
                startedOperationCount = startedOperationCount,
                completedOperationCount = completedOperationCount,
                failedOperationCount = failedOperationCount,
                addedAt = now()
            )
        }
    }
}