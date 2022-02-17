package altchain.network.monitor.tool.persistence.tables

import altchain.network.monitor.tool.persistence.varchar
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object PopSubsidiesMonitorTable : Table("pop_subsidies_monitor") {
    val networkId = varchar("network_id")
    val id = varchar("id")
    val version = varchar("version")
    val host = varchar("host")
    val startedOperationCount = integer("started_operation_count")
    val completedOperationCount = integer("completed_operation_count")
    val failedOperationCount = integer("failed_operation_count")
    val addedAt = timestamp("added_at")
}

data class PopSubsidiesMonitorRecord(
    val networkId: String,
    val id: String,
    val version: String,
    val host: String,
    val startedOperationCount: Int,
    val completedOperationCount: Int,
    val failedOperationCount: Int,
    val addedAt: Instant
)

data class PopSubsidiesMonitor(
    val version: String,
    val startedOperationCount: Int,
    val completedOperationCount: Int,
    val failedOperationCount: Int,
    val addedAt: Instant
)

fun ResultRow.toPopSubsidiesMonitorRecord(): PopSubsidiesMonitorRecord = PopSubsidiesMonitorRecord(
    networkId = this[PopSubsidiesMonitorTable.networkId],
    id = this[PopSubsidiesMonitorTable.id],
    version = this[PopSubsidiesMonitorTable.version],
    host = this[PopSubsidiesMonitorTable.host],
    startedOperationCount = this[PopSubsidiesMonitorTable.startedOperationCount],
    completedOperationCount = this[PopSubsidiesMonitorTable.completedOperationCount],
    failedOperationCount = this[PopSubsidiesMonitorTable.failedOperationCount],
    addedAt = this[PopSubsidiesMonitorTable.addedAt].toKotlinInstant()
)