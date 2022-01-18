package altchain.network.monitor.tool.persistence.tables

import altchain.network.monitor.tool.MetricType
import altchain.network.monitor.tool.persistence.bigVarchar
import altchain.network.monitor.tool.persistence.enumerationByName
import altchain.network.monitor.tool.persistence.varchar
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object MinerMonitorTable : Table("miner_monitor") {
    val networkId = varchar("network_id")
    val minerId = varchar("miner_id")
    val minerVersion = varchar("miner_version")
    val host = varchar("host")
    val minerType = enumerationByName("miner_type", MinerType::class)
    val altchainKey = varchar("altchain_key").nullable()
    val startedOperationCount = integer("started_operation_count")
    val completedOperationCount = integer("completed_operation_count")
    val failedOperationCount = integer("failed_operation_count")
    val isMining = bool("is_mining")
    val minerDiagnostic = bigVarchar("miner_diagnostic")
    val metrics = bigVarchar("metrics")
    val uptimeSeconds = integer("uptime_seconds")
    val addedAt = timestamp("added_at")
}

data class MinerMonitorRecord(
    val networkId: String,
    val minerId: String,
    val minerVersion: String,
    val host: String,
    val minerType: MinerType,
    val altchainKey: String? = null,
    val startedOperationCount: Int,
    val completedOperationCount: Int,
    val failedOperationCount: Int,
    val isMining: Boolean,
    val minerDiagnostic: Set<String>,
    val metrics: MetricsRecord,
    val uptimeSeconds: Int,
    val addedAt: Instant
)

data class MinerMonitor(
    val minerVersion: String,
    val startedOperationCount: Int,
    val completedOperationCount: Int,
    val failedOperationCount: Int,
    val isMining: Boolean,
    val minerDiagnostic: Set<String>,
    val metrics: MetricsRecord,
    val uptimeSeconds: Int,
    val addedAt: Instant
)

@Serializable
data class MetricsRecord(
    val metrics: Set<MetricRecord>
)

@Serializable
data class MetricRecord(
    val type: MetricType,
    val value: String
)

enum class MinerType {
    VPM,
    APM
}

fun ResultRow.toMinerMonitorRecord(): MinerMonitorRecord = MinerMonitorRecord(
    networkId = this[MinerMonitorTable.networkId],
    minerId = this[MinerMonitorTable.minerId],
    minerVersion = this[MinerMonitorTable.minerVersion],
    host = this[MinerMonitorTable.host],
    minerType = this[MinerMonitorTable.minerType],
    altchainKey = this[MinerMonitorTable.altchainKey],
    startedOperationCount = this[MinerMonitorTable.startedOperationCount],
    completedOperationCount = this[MinerMonitorTable.completedOperationCount],
    failedOperationCount = this[MinerMonitorTable.failedOperationCount],
    isMining = this[MinerMonitorTable.isMining],
    minerDiagnostic = this[MinerMonitorTable.minerDiagnostic].toDiagnostics(),
    metrics = Json.decodeFromString(this[MinerMonitorTable.metrics]),
    uptimeSeconds = this[MinerMonitorTable.uptimeSeconds],
    addedAt = this[MinerMonitorTable.addedAt].toKotlinInstant()
)

fun String.toDiagnostics(): Set<String> = split(",").filterNot {
    it.isEmpty()
}.asSequence().map {
    it
}.toSet()
