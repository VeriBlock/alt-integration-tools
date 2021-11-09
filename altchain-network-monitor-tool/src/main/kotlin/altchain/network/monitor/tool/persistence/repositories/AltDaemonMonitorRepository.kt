package altchain.network.monitor.tool.persistence.repositories

import altchain.network.monitor.tool.persistence.tables.AltDaemonMonitor
import altchain.network.monitor.tool.persistence.tables.AltDaemonMonitorRecord
import altchain.network.monitor.tool.persistence.tables.AltDaemonMonitorTable
import altchain.network.monitor.tool.persistence.tables.toAltDaemonMonitorRecord
import altchain.network.monitor.tool.util.now
import kotlinx.datetime.toJavaInstant
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class AltDaemonMonitorRepository(
    private val database: Database
) {
    fun create(networkId: String, altDaemonId: String, host: String, monitor: AltDaemonMonitor) {
        transaction(database) {
            AltDaemonMonitorTable.insert {
                it[this.networkId] = networkId
                it[this.altDaemonId] = altDaemonId
                it[this.host] = host
                it[localHeight] = monitor.localHeight
                it[networkHeight] = monitor.networkHeight
                it[isSynchronized] = monitor.isSynchronized
                it[addedAt] = monitor.addedAt.toJavaInstant()
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    fun find(networkId: String, altDaemonIds: Set<String>): List<AltDaemonMonitorRecord> = transaction(database) {
        AltDaemonMonitorTable.select {
            (AltDaemonMonitorTable.networkId.lowerCase() eq networkId.lowercase()) and
                    (AltDaemonMonitorTable.altDaemonId.lowerCase() inList altDaemonIds)
        }.orderBy(
            AltDaemonMonitorTable.addedAt,
            SortOrder.DESC
        ).distinctBy {
            it[AltDaemonMonitorTable.networkId]
            it[AltDaemonMonitorTable.altDaemonId]
        }.map {
            it.toAltDaemonMonitorRecord()
        }
    }

    @OptIn(ExperimentalTime::class)
    fun deleteOldData(hours: Int): Int = transaction {
        val initialTime = now().minus(Duration.hours(hours)).toJavaInstant()
        AltDaemonMonitorTable.deleteWhere {
            (AltDaemonMonitorTable.addedAt lessEq initialTime)
        }
    }
}