package altchain.network.monitor.tool.persistence.repositories

import altchain.network.monitor.tool.persistence.tables.PopSubsidiesMonitor
import altchain.network.monitor.tool.persistence.tables.PopSubsidiesMonitorRecord
import altchain.network.monitor.tool.persistence.tables.PopSubsidiesMonitorTable
import altchain.network.monitor.tool.persistence.tables.toPopSubsidiesMonitorRecord
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

class PopSubsidiesRepository(
    private val database: Database
) {
    fun create(networkId: String, id: String, host: String, monitor: PopSubsidiesMonitor) {
        transaction(database) {
            PopSubsidiesMonitorTable.insert {
                it[this.networkId] = networkId
                it[this.id] = id
                it[version] = monitor.version
                it[this.host] = host
                it[startedOperationCount] = monitor.startedOperationCount
                it[completedOperationCount] = monitor.completedOperationCount
                it[failedOperationCount] = monitor.failedOperationCount
                it[addedAt] = monitor.addedAt.toJavaInstant()
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    fun find(networkId: String, ids: Set<String>): List<PopSubsidiesMonitorRecord> = transaction(database) {
        PopSubsidiesMonitorTable.select {
            (PopSubsidiesMonitorTable.networkId.lowerCase() eq networkId.lowercase()) and
                    (PopSubsidiesMonitorTable.id.lowerCase() inList ids)
        }.orderBy(
            PopSubsidiesMonitorTable.addedAt,
            SortOrder.DESC
        ).distinctBy {
            it[PopSubsidiesMonitorTable.networkId]
            it[PopSubsidiesMonitorTable.id]
        }.map {
            it.toPopSubsidiesMonitorRecord()
        }
    }

    @OptIn(ExperimentalTime::class)
    fun deleteOldData(hours: Int): Int = transaction {
        val initialTime = now().minus(Duration.hours(hours)).toJavaInstant()
        PopSubsidiesMonitorTable.deleteWhere {
            (PopSubsidiesMonitorTable.addedAt lessEq initialTime)
        }
    }
}