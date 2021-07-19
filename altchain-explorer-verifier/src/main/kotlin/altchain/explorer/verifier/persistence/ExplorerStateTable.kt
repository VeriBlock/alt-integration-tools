package altchain.explorer.verifier.persistence

import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.`java-time`.timestamp

object ExplorerStateTable : Table("explorer_state") {
    val configName = varchar("config_name", 255)
    val url = varchar("url", 255)
    val blockCount = integer("block_count")
    val atvCount = integer("atv_count")
    val vtbCount = integer("vtb_count")
    val vbkCount = integer("vbk_count")
    val atvBlocks = varchar("atv_blocks", 5000)
    val vtbBlocks = varchar("vtb_blocks", 5000)
    val vbkBlocks = varchar("vbk_blocks", 5000)
    val addedAt = timestamp("added_at")
}

data class ExplorerState(
    val configName: String,
    val url: String,
    val blockCount: Int,
    val atvCount: Int,
    val vtbCount: Int,
    val vbkCount: Int,
    val atvBlocks: Set<Int>,
    val vtbBlocks: Set<Int>,
    val vbkBlocks: Set<Int>,
    val addedAt: Instant
)

fun ResultRow.toExplorerState(): ExplorerState = ExplorerState(
    this[ExplorerStateTable.configName],
    this[ExplorerStateTable.url],
    this[ExplorerStateTable.blockCount],
    this[ExplorerStateTable.atvCount],
    this[ExplorerStateTable.vtbCount],
    this[ExplorerStateTable.vbkCount],
    this[ExplorerStateTable.atvBlocks].toBlockHeights(),
    this[ExplorerStateTable.vtbBlocks].toBlockHeights(),
    this[ExplorerStateTable.vbkBlocks].toBlockHeights(),
    this[ExplorerStateTable.addedAt].toKotlinInstant()
)

private fun String.toBlockHeights(): Set<Int> = split(",").filterNot {
    it.isEmpty()
}.asSequence().map {
    it.toInt()
}.toSet()