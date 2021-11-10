package altchain.network.monitor.tool.persistence.tables

import altchain.network.monitor.tool.persistence.varchar
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.`java-time`.timestamp

object VbfiMonitorTable : Table("vbfi_monitor") {
    val networkId = varchar("network_id")
    val vbfiId = varchar("vbfi_id")
    val vbfiVersion = varchar("vbfi_version")
    val host = varchar("host")
    val lastBlockHeight = integer("last_block_height")
    val lastExplorerBlockHeight = integer("last_explorer_block_height")
    val isSynchronized = bool("is_synchronized")
    val addedAt = timestamp("added_at")
}

data class VbfiMonitorRecord(
    val networkId: String,
    val vbfiId: String,
    val vbfiVersion: String,
    val host: String,
    val lastBlockHeight: Int,
    val lastExplorerBlockHeight: Int,
    val isSynchronized: Boolean,
    val addedAt: Instant
)

data class VbfiMonitor(
    val vbfiVersion: String,
    val lastBlockHeight: Int,
    val lastExplorerBlockHeight: Int,
    val isSynchronized: Boolean,
    val addedAt: Instant
)

fun ResultRow.toVbfiMonitorRecord(): VbfiMonitorRecord = VbfiMonitorRecord(
    networkId = this[VbfiMonitorTable.networkId],
    vbfiId = this[VbfiMonitorTable.vbfiId],
    vbfiVersion = this[VbfiMonitorTable.vbfiVersion],
    host = this[VbfiMonitorTable.host],
    lastBlockHeight = this[VbfiMonitorTable.lastBlockHeight],
    lastExplorerBlockHeight = this[VbfiMonitorTable.lastExplorerBlockHeight],
    isSynchronized = this[VbfiMonitorTable.isSynchronized],
    addedAt = this[VbfiMonitorTable.addedAt].toKotlinInstant()
)