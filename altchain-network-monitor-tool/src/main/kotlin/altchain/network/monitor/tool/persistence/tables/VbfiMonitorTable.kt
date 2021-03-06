package altchain.network.monitor.tool.persistence.tables

import altchain.network.monitor.tool.persistence.varchar
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object VbfiMonitorTable : Table("vbfi_monitor") {
    val networkId = varchar("network_id")
    val id = varchar("id")
    val version = varchar("version")
    val host = varchar("host")
    val lastBlockHeight = integer("last_block_height")
    val lastBlockFinalizedBtcHeight = integer("last_block_finalized_btc_height")
    val lastExplorerBlockHeight = integer("last_explorer_block_height")
    val isLastBlockSynchronized = bool("is_last_block_synchronized")
    val isLastBlockFinalizedBtcSynchronized = bool("is_last_block_finalized_btc_synchronized")
    val addedAt = timestamp("added_at")
}

data class VbfiMonitorRecord(
    val networkId: String,
    val id: String,
    val version: String,
    val host: String,
    val lastBlockHeight: Int,
    val lastBlockFinalizedBtcHeight: Int,
    val lastExplorerBlockHeight: Int,
    val isLastBlockSynchronized: Boolean,
    val isLastBlockFinalizedBtcSynchronized: Boolean,
    val addedAt: Instant
)

data class VbfiMonitor(
    val vbfiVersion: String,
    val lastBlockHeight: Int,
    val lastBlockFinalizedBtcHeight: Int,
    val lastExplorerBlockHeight: Int,
    val isLastBlockSynchronized: Boolean,
    val isLastBlockFinalizedBtcSynchronized: Boolean,
    val addedAt: Instant
)

fun ResultRow.toVbfiMonitorRecord(): VbfiMonitorRecord = VbfiMonitorRecord(
    networkId = this[VbfiMonitorTable.networkId],
    id = this[VbfiMonitorTable.id],
    version = this[VbfiMonitorTable.version],
    host = this[VbfiMonitorTable.host],
    lastBlockHeight = this[VbfiMonitorTable.lastBlockHeight],
    lastBlockFinalizedBtcHeight = this[VbfiMonitorTable.lastBlockFinalizedBtcHeight],
    lastExplorerBlockHeight = this[VbfiMonitorTable.lastExplorerBlockHeight],
    isLastBlockSynchronized = this[VbfiMonitorTable.isLastBlockSynchronized],
    isLastBlockFinalizedBtcSynchronized = this[VbfiMonitorTable.isLastBlockFinalizedBtcSynchronized],
    addedAt = this[VbfiMonitorTable.addedAt].toKotlinInstant()
)