package altchain.network.monitor.tool.persistence.repositories

import altchain.network.monitor.tool.persistence.tables.ExplorerMonitor
import altchain.network.monitor.tool.persistence.tables.ExplorerMonitorRecord
import altchain.network.monitor.tool.persistence.tables.ExplorerMonitorTable
import altchain.network.monitor.tool.persistence.tables.toExplorerMonitorRecord
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

class ExplorerMonitorRepository(
    private val database: Database
) {
    fun create(networkId: String, id: String, host: String, monitor: ExplorerMonitor) {
        transaction(database) {
            ExplorerMonitorTable.insert {
                it[this.networkId] = networkId
                it[this.id] = id
                it[this.host] = host
                it[blockCount] = monitor.blockCount
                it[atvCount] = monitor.atvCount
                it[vtbCount] = monitor.vtbCount
                it[vbkCount] = monitor.vbkCount
                it[atvBlocks] = monitor.atvBlocks.joinToString(",")
                it[vtbBlocks] = monitor.vtbBlocks.joinToString(",")
                it[vbkBlocks] = monitor.vbkBlocks.joinToString(",")
                it[addedAt] = monitor.addedAt.toJavaInstant()
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    fun find(networkId: String, ids: Set<String>): List<ExplorerMonitorRecord> = transaction(database) {
        ExplorerMonitorTable.select {
            (ExplorerMonitorTable.networkId.lowerCase() eq networkId.lowercase()) and
                    (ExplorerMonitorTable.id.lowerCase() inList ids)
        }.orderBy(
            ExplorerMonitorTable.addedAt,
            SortOrder.DESC
        ).distinctBy {
            it[ExplorerMonitorTable.networkId]
            it[ExplorerMonitorTable.id]
        }.map {
            it.toExplorerMonitorRecord()
        }
    }

    @OptIn(ExperimentalTime::class)
    fun deleteOldData(hours: Int): Int = transaction {
        val initialTime = now().minus(Duration.hours(hours)).toJavaInstant()
        ExplorerMonitorTable.deleteWhere {
            (ExplorerMonitorTable.addedAt lessEq initialTime)
        }
    }
}