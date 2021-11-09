package altchain.network.monitor.tool.persistence.repositories

import altchain.network.monitor.tool.persistence.tables.NodeCoreMonitor
import altchain.network.monitor.tool.persistence.tables.NodeCoreMonitorRecord
import altchain.network.monitor.tool.persistence.tables.NodeCoreMonitorTable
import altchain.network.monitor.tool.persistence.tables.toNodeCoreMonitorRecord
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

class NodeCoreMonitorRepository(
    private val database: Database
) {
    fun create(networkId: String, nodecoreId: String, host: String, monitor: NodeCoreMonitor) {
        transaction(database) {
            NodeCoreMonitorTable.insert {
                it[this.networkId] = networkId
                it[this.nodecoreId] = nodecoreId
                it[nodecoreVersion] = monitor.nodecoreVersion
                it[this.host] = host
                it[localHeight] = monitor.localHeight
                it[networkHeight] = monitor.networkHeight
                it[isSynchronized] = monitor.isSynchronized
                it[addedAt] = monitor.addedAt.toJavaInstant()
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    fun find(networkId: String, nodecoreIds: Set<String>): List<NodeCoreMonitorRecord> = transaction(database) {
        NodeCoreMonitorTable.select {
            (NodeCoreMonitorTable.networkId.lowerCase() eq networkId.lowercase()) and
                    (NodeCoreMonitorTable.nodecoreId.lowerCase() inList nodecoreIds)
        }.orderBy(
            NodeCoreMonitorTable.addedAt,
            SortOrder.DESC
        ).distinctBy {
            it[NodeCoreMonitorTable.networkId]
            it[NodeCoreMonitorTable.nodecoreId]
        }.map {
            it.toNodeCoreMonitorRecord()
        }
    }

    @OptIn(ExperimentalTime::class)
    fun deleteOldData(hours: Int): Int = transaction {
        val initialTime = now().minus(Duration.hours(hours)).toJavaInstant()
        NodeCoreMonitorTable.deleteWhere {
            (NodeCoreMonitorTable.addedAt lessEq initialTime)
        }
    }
}