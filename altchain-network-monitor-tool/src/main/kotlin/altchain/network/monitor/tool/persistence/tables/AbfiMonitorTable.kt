package altchain.network.monitor.tool.persistence.tables

import altchain.network.monitor.tool.persistence.bigVarchar
import altchain.network.monitor.tool.persistence.varchar
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object AbfiMonitorTable : Table("abfi_monitor") {
    val networkId = varchar("network_id")
    val id = varchar("id")
    val version = varchar("version")
    val host = varchar("host")
    val prefix = varchar("prefix")
    val blockInfo = bigVarchar("block_info")
    val lastFinalizedBlockHeight = integer("last_finalized_block_height")
    val lastNetworkBlockHeight = integer("last_network_block_height")
    val haveLastFinalizedBlockBtc = bool("have_last_finalized_block_btc")
    val isSynchronized = bool("is_synchronized")
    val diagnostic = bigVarchar("diagnostic")
    val addedAt = timestamp("added_at")
}

data class AbfiMonitorRecord(
    val networkId: String,
    val abfiId: String,
    val abfiVersion: String,
    val host: String,
    val prefix: String,
    val blockInfo: AbfiBlocksRecord,
    val lastFinalizedBlockHeight: Int,
    val lastNetworkBlockHeight: Int,
    val haveLastFinalizedBlockBtc: Boolean,
    val isSynchronized: Boolean,
    val diagnostic: String,
    val addedAt: Instant
)

data class AbfiMonitor(
    val abfiVersion: String,
    val blockInfo: AbfiBlocksRecord,
    val lastFinalizedBlockHeight: Int,
    val lastNetworkBlockHeight: Int,
    val haveLastFinalizedBlockBtc: Boolean,
    val isSynchronized: Boolean,
    val diagnostic: String,
    val addedAt: Instant
)

@Serializable
data class AbfiBlocksRecord(
    val blocks: List<AbfiBlockRecord>
)

@Serializable
data class AbfiBlockRecord(
    val name: String,
    val blockInfo: AbfiBlockInfoRecord
)

@Serializable
data class AbfiBlockInfoRecord(
    val height: Int?,
    val spFinality: Int?,
    val bitcoinFinality: Int?,
    val endorsedInHeight: Int?,
    val verifiedInHeight: Int?
)

fun ResultRow.toAbfiMonitorRecord(): AbfiMonitorRecord = AbfiMonitorRecord(
    networkId = this[AbfiMonitorTable.networkId],
    abfiId = this[AbfiMonitorTable.id],
    abfiVersion = this[AbfiMonitorTable.version],
    host = this[AbfiMonitorTable.host],
    prefix = this[AbfiMonitorTable.prefix],
    blockInfo = Json.decodeFromString(this[AbfiMonitorTable.blockInfo]),
    lastFinalizedBlockHeight = this[AbfiMonitorTable.lastFinalizedBlockHeight],
    lastNetworkBlockHeight = this[AbfiMonitorTable.lastNetworkBlockHeight],
    haveLastFinalizedBlockBtc = this[AbfiMonitorTable.haveLastFinalizedBlockBtc],
    isSynchronized = this[AbfiMonitorTable.isSynchronized],
    diagnostic = this[AbfiMonitorTable.diagnostic],
    addedAt = this[AbfiMonitorTable.addedAt].toKotlinInstant()
)