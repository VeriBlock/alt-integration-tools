package altchain.explorer.verifier.persistence

import kotlinx.datetime.toJavaInstant
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class ExplorerStateRepository(
    private val database: Database
) {
    fun addExplorerState(explorerState: ExplorerState) = transaction(database) {
        ExplorerStateTable.insert {
            it[configName] = explorerState.configName
            it[url] = explorerState.url
            it[blockCount] = explorerState.blockCount
            it[atvCount] = explorerState.atvCount
            it[vtbCount] = explorerState.vtbCount
            it[vbkCount] = explorerState.vbkCount
            it[atvBlocks] = explorerState.atvBlocks.joinToString(",")
            it[vtbBlocks] = explorerState.vtbBlocks.joinToString(",")
            it[vbkBlocks] = explorerState.vbkBlocks.joinToString(",")
            it[addedAt] = explorerState.addedAt.toJavaInstant()
        }
    }

    fun delete(configName: String): Int = transaction(database) {
        ExplorerStateTable.deleteWhere {
            (ExplorerStateTable.configName.lowerCase() eq configName.lowercase())
        }
    }

    fun getLatestExplorerState(configName: String): ExplorerState? = transaction(database) {
        ExplorerStateTable.select {
            (ExplorerStateTable.configName.lowerCase() eq configName.lowercase())
        }.orderBy(
            ExplorerStateTable.addedAt,
            SortOrder.DESC
        ).firstOrNull()?.toExplorerState()
    }

    fun getLatestExplorerStates(): Set<ExplorerState> = transaction(database) {
        ExplorerStateTable.selectAll().orderBy(
            ExplorerStateTable.addedAt,
            SortOrder.DESC
        ).distinctBy {
            it[ExplorerStateTable.configName]
        }.asSequence().map {
            it.toExplorerState()
        }.toSet()
    }
}