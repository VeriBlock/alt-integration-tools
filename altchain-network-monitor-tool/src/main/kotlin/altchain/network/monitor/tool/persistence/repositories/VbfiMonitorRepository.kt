package altchain.network.monitor.tool.persistence.repositories

import altchain.network.monitor.tool.persistence.tables.VbfiMonitor
import altchain.network.monitor.tool.persistence.tables.VbfiMonitorRecord
import altchain.network.monitor.tool.persistence.tables.VbfiMonitorTable
import altchain.network.monitor.tool.persistence.tables.toVbfiMonitorRecord
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

class VbfiMonitorRepository(
    private val database: Database
) {
    fun create(networkId: String, vbfiId: String, host: String, monitor: VbfiMonitor) {
        transaction(database) {
            VbfiMonitorTable.insert {
                it[this.networkId] = networkId
                it[this.vbfiId] = vbfiId
                it[vbfiVersion] = monitor.vbfiVersion
                it[this.host] = host
                it[lastBlockHeight] = monitor.lastBlockHeight
                it[lastExplorerBlockHeight] = monitor.lastExplorerBlockHeight
                it[isSynchronized] = monitor.isSynchronized
                it[addedAt] = monitor.addedAt.toJavaInstant()
            }
        }
    }

    fun find(networkId: String, vbfiIds: Set<String>): List<VbfiMonitorRecord> = transaction(database) {
        VbfiMonitorTable.select {
            (VbfiMonitorTable.networkId.lowerCase() eq networkId.lowercase()) and
                    (VbfiMonitorTable.vbfiId.lowerCase() inList vbfiIds)
        }.orderBy(
            column = VbfiMonitorTable.addedAt,
            order = SortOrder.DESC
        ).distinctBy {
            it[VbfiMonitorTable.networkId]
            it[VbfiMonitorTable.vbfiId]
        }.map {
            it.toVbfiMonitorRecord()
        }
    }

    @OptIn(ExperimentalTime::class)
    fun deleteOldData(hours: Int): Int = transaction {
        val initialTime = now().minus(Duration.hours(hours)).toJavaInstant()
        VbfiMonitorTable.deleteWhere {
            (VbfiMonitorTable.addedAt lessEq initialTime)
        }
    }
}