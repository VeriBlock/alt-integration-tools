package altchain.network.monitor.tool.persistence.repositories

import altchain.network.monitor.tool.persistence.tables.MinerMonitor
import altchain.network.monitor.tool.persistence.tables.MinerMonitorRecord
import altchain.network.monitor.tool.persistence.tables.MinerMonitorTable
import altchain.network.monitor.tool.persistence.tables.MinerType
import altchain.network.monitor.tool.persistence.tables.toMinerMonitorRecord
import altchain.network.monitor.tool.util.now
import kotlinx.datetime.Instant
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

class MinerMonitorRepository(
    private val database: Database
) {
    fun create(networkId: String, id: String, host: String, type: MinerType, monitor: MinerMonitor) {
        transaction(database) {
            MinerMonitorTable.insert {
                it[this.networkId] = networkId
                it[this.id] = id
                it[version] = monitor.version
                it[this.host] = host
                it[this.minerType] = type
                it[startedOperationCount] = monitor.startedOperationCount
                it[completedOperationCount] = monitor.completedOperationCount
                it[failedOperationCount] = monitor.failedOperationCount
                it[isMining] = monitor.isMining
                it[minerDiagnostic] = monitor.minerDiagnostic.joinToString(",")
                it[metrics] = Json.encodeToString(monitor.metrics)
                it[uptimeSeconds] = monitor.uptimeSeconds
                it[addedAt] = monitor.addedAt.toJavaInstant()
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    fun find(networkId: String, ids: Set<String>): List<MinerMonitorRecord> = transaction(database) {
        MinerMonitorTable.select {
            (MinerMonitorTable.networkId.lowerCase() eq networkId.lowercase()) and
                    (MinerMonitorTable.id.lowerCase() inList ids)
        }.orderBy(
            MinerMonitorTable.addedAt,
            SortOrder.DESC
        ).distinctBy {
            it[MinerMonitorTable.networkId]
            it[MinerMonitorTable.id]
        }.map {
            it.toMinerMonitorRecord()
        }
    }

    fun findLatests(networkId: String, type: MinerType, id: String, limit: Int = 2, startDate: Instant): List<MinerMonitorRecord> = transaction(database) {
        MinerMonitorTable.select {
            (MinerMonitorTable.networkId.lowerCase() eq networkId.lowercase()) and
                    (MinerMonitorTable.id eq id.lowercase()) and
                    (MinerMonitorTable.minerType eq type) and
                    (MinerMonitorTable.addedAt greaterEq startDate.toJavaInstant())
        }.orderBy(
            MinerMonitorTable.addedAt,
            SortOrder.DESC
        ).limit(
            n = limit
        ).map {
            it.toMinerMonitorRecord()
        }
    }

    @OptIn(ExperimentalTime::class)
    fun deleteOldData(hours: Int): Int = transaction {
        val initialTime = now().minus(Duration.hours(hours)).toJavaInstant()
        MinerMonitorTable.deleteWhere {
            (MinerMonitorTable.addedAt lessEq initialTime)
        }
    }
}