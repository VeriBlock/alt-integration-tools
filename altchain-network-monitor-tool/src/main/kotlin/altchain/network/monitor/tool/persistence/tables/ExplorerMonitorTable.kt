package altchain.network.monitor.tool.persistence.tables

import altchain.network.monitor.tool.persistence.bigVarchar
import altchain.network.monitor.tool.persistence.varchar
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object ExplorerMonitorTable : Table("explorer_monitor") {
    val networkId = varchar("network_id")
    val explorerId = varchar("explorer_id")
    val host = varchar("host")
    val blockCount = integer("block_count")
    val atvCount = integer("atv_count")
    val vtbCount = integer("vtb_count")
    val vbkCount = integer("vbk_count")
    val atvBlocks = bigVarchar("atv_blocks")
    val vtbBlocks = bigVarchar("vtb_blocks")
    val vbkBlocks = bigVarchar("vbk_blocks")
    val addedAt = timestamp("added_at")
}

data class ExplorerMonitorRecord(
    val networkId: String,
    val explorerId: String,
    val host: String,
    val blockCount: Int,
    val atvCount: Int,
    val vtbCount: Int,
    val vbkCount: Int,
    val atvBlocks: Set<Int>,
    val vtbBlocks: Set<Int>,
    val vbkBlocks: Set<Int>,
    val addedAt: Instant
)

data class ExplorerMonitor(
    val blockCount: Int,
    val atvCount: Int,
    val vtbCount: Int,
    val vbkCount: Int,
    val atvBlocks: Set<Int>,
    val vtbBlocks: Set<Int>,
    val vbkBlocks: Set<Int>,
    val addedAt: Instant
)

fun ResultRow.toExplorerMonitorRecord(): ExplorerMonitorRecord = ExplorerMonitorRecord(
    this[ExplorerMonitorTable.networkId],
    this[ExplorerMonitorTable.explorerId],
    this[ExplorerMonitorTable.host],
    this[ExplorerMonitorTable.blockCount],
    this[ExplorerMonitorTable.atvCount],
    this[ExplorerMonitorTable.vtbCount],
    this[ExplorerMonitorTable.vbkCount],
    this[ExplorerMonitorTable.atvBlocks].toBlockHeights(),
    this[ExplorerMonitorTable.vtbBlocks].toBlockHeights(),
    this[ExplorerMonitorTable.vbkBlocks].toBlockHeights(),
    this[ExplorerMonitorTable.addedAt].toKotlinInstant()
)

private fun String.toBlockHeights(): Set<Int> = split(",").filterNot {
    it.isEmpty()
}.asSequence().map {
    it.toInt()
}.toSet()