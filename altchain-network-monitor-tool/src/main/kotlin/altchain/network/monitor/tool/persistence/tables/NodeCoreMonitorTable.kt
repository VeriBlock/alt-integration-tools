package altchain.network.monitor.tool.persistence.tables

import altchain.network.monitor.tool.persistence.varchar
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object NodeCoreMonitorTable : Table("nodecore_monitor") {
    val networkId = varchar("network_id")
    val id = varchar("id")
    val version = varchar("version")
    val host = varchar("host")
    val localHeight = integer("local_height")
    val networkHeight = integer("network_height")
    val isSynchronized = bool("is_synchronized")
    val addedAt = timestamp("added_at")
}

data class NodeCoreMonitorRecord(
    val networkId: String,
    val id: String,
    val version: String,
    val host: String,
    val localHeight: Int,
    val networkHeight: Int,
    val isSynchronized: Boolean,
    val addedAt: Instant
)

data class NodeCoreMonitor(
    val version: String,
    val localHeight: Int,
    val networkHeight: Int,
    val isSynchronized: Boolean,
    val addedAt: Instant
)

fun ResultRow.toNodeCoreMonitorRecord(): NodeCoreMonitorRecord = NodeCoreMonitorRecord(
    this[NodeCoreMonitorTable.networkId],
    this[NodeCoreMonitorTable.id],
    this[NodeCoreMonitorTable.version],
    this[NodeCoreMonitorTable.host],
    this[NodeCoreMonitorTable.localHeight],
    this[NodeCoreMonitorTable.networkHeight],
    this[NodeCoreMonitorTable.isSynchronized],
    this[NodeCoreMonitorTable.addedAt].toKotlinInstant()
)