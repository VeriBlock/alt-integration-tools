package altchain.network.monitor.tool.persistence.repositories

import altchain.network.monitor.tool.persistence.tables.AbfiMonitor
import altchain.network.monitor.tool.persistence.tables.AbfiMonitorRecord
import altchain.network.monitor.tool.persistence.tables.AbfiMonitorTable
import altchain.network.monitor.tool.persistence.tables.toAbfiMonitorRecord
import altchain.network.monitor.tool.util.now
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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

class AbfiMonitorRepository(
    private val database: Database
) {
    fun create(networkId: String, abfiId: String, host: String, prefix: String, monitor: AbfiMonitor) {
        transaction(database) {
            AbfiMonitorTable.insert {
                it[this.networkId] = networkId
                it[this.abfiId] = abfiId
                it[abfiVersion] = monitor.abfiVersion
                it[this.host] = host
                it[this.prefix] = prefix
                it[blockInfo] = Json.encodeToString(monitor.blockInfo)
                it[lastFinalizedBlockHeight] = monitor.lastFinalizedBlockHeight
                it[lastNetworkBlockHeight] = monitor.lastNetworkBlockHeight
                it[haveLastFinalizedBlockBtc] = monitor.haveLastFinalizedBlockBtc
                it[isSynchronized] = monitor.isSynchronized
                it[diagnostic] = monitor.diagnostic
                it[addedAt] = monitor.addedAt.toJavaInstant()
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    fun find(networkId: String, abfiIds: Set<String>): List<AbfiMonitorRecord> = transaction(database) {
        AbfiMonitorTable.select {
            (AbfiMonitorTable.networkId.lowerCase() eq networkId.lowercase()) and
                    (AbfiMonitorTable.abfiId.lowerCase() inList abfiIds)
        }.orderBy(
            column = AbfiMonitorTable.addedAt,
            order = SortOrder.DESC
        ).distinctBy {
            it[AbfiMonitorTable.networkId]
            it[AbfiMonitorTable.abfiId]
        }.map {
            it.toAbfiMonitorRecord()
        }
    }

    @OptIn(ExperimentalTime::class)
    fun deleteOldData(hours: Int): Int = transaction {
        val initialTime = now().minus(Duration.hours(hours)).toJavaInstant()
        AbfiMonitorTable.deleteWhere {
            (AbfiMonitorTable.addedAt lessEq initialTime)
        }
    }
}