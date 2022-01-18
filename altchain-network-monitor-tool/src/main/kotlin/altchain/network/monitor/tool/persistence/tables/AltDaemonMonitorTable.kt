package altchain.network.monitor.tool.persistence.tables

import altchain.network.monitor.tool.persistence.varchar
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object AltDaemonMonitorTable : Table("alt_daemon_monitor") {
    val networkId = varchar("network_id")
    val altDaemonId = varchar("alt_daemon_id")
    val host = varchar("host")
    val localHeight = integer("local_height")
    val networkHeight = integer("network_height")
    val isSynchronized = bool("is_synchronized")
    val addedAt = timestamp("added_at")
}

data class AltDaemonMonitorRecord(
    val networkId: String,
    val altDaemonId: String,
    val host: String,
    val localHeight: Int,
    val networkHeight: Int,
    val isSynchronized: Boolean,
    val addedAt: Instant
)

data class AltDaemonMonitor(
    val host: String,
    val localHeight: Int,
    val networkHeight: Int,
    val isSynchronized: Boolean,
    val addedAt: Instant
)

fun ResultRow.toAltDaemonMonitorRecord(): AltDaemonMonitorRecord = AltDaemonMonitorRecord(
    this[AltDaemonMonitorTable.networkId],
    this[AltDaemonMonitorTable.altDaemonId],
    this[AltDaemonMonitorTable.host],
    this[AltDaemonMonitorTable.localHeight],
    this[AltDaemonMonitorTable.networkHeight],
    this[AltDaemonMonitorTable.isSynchronized],
    this[AltDaemonMonitorTable.addedAt].toKotlinInstant()
)