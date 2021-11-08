package altchain.network.monitor.tool.persistence.tables

import altchain.network.monitor.tool.persistence.varchar
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.`java-time`.timestamp

object NodeCoreMonitorTable : Table("nodecore_monitor") {
    val networkId = varchar("network_id")
    val nodecoreId = varchar("nodecore_id")
    val nodecoreVersion = varchar("nodecore_version")
    val host = varchar("host")
    val localHeight = integer("local_height")
    val networkHeight = integer("network_height")
    val isSynchronized = bool("is_synchronized")
    val addedAt = timestamp("added_at")
}

data class NodeCoreMonitorRecord(
    val networkId: String,
    val nodecoreId: String,
    val nodecoreVersion: String,
    val host: String,
    val localHeight: Int,
    val networkHeight: Int,
    val isSynchronized: Boolean,
    val addedAt: Instant
)

data class NodeCoreMonitor(
    val nodecoreVersion: String,
    val localHeight: Int,
    val networkHeight: Int,
    val isSynchronized: Boolean,
    val addedAt: Instant
)

fun ResultRow.toNodeCoreMonitorRecord(): NodeCoreMonitorRecord = NodeCoreMonitorRecord(
    this[NodeCoreMonitorTable.networkId],
    this[NodeCoreMonitorTable.nodecoreId],
    this[NodeCoreMonitorTable.nodecoreVersion],
    this[NodeCoreMonitorTable.host],
    this[NodeCoreMonitorTable.localHeight],
    this[NodeCoreMonitorTable.networkHeight],
    this[NodeCoreMonitorTable.isSynchronized],
    this[NodeCoreMonitorTable.addedAt].toKotlinInstant()
)