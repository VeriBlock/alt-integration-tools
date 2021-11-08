package altchain.network.monitor.tool.persistence

import altchain.network.monitor.tool.persistence.repositories.AbfiMonitorRepository
import altchain.network.monitor.tool.persistence.repositories.AltDaemonMonitorRepository
import altchain.network.monitor.tool.persistence.repositories.ExplorerMonitorRepository
import altchain.network.monitor.tool.persistence.repositories.GlobalVariableRepository
import altchain.network.monitor.tool.persistence.repositories.MinerMonitorRepository
import altchain.network.monitor.tool.persistence.repositories.NodeCoreMonitorRepository
import altchain.network.monitor.tool.persistence.tables.AbfiMonitorTable
import altchain.network.monitor.tool.persistence.tables.AltDaemonMonitorTable
import altchain.network.monitor.tool.persistence.tables.ExplorerMonitorTable
import altchain.network.monitor.tool.persistence.tables.GlobalVariableTable
import altchain.network.monitor.tool.persistence.tables.MinerMonitorTable
import altchain.network.monitor.tool.persistence.tables.NodeCoreMonitorTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import org.koin.core.module.Module
import org.koin.dsl.module
import java.sql.Connection
import javax.sql.DataSource
import org.veriblock.core.utilities.Configuration

data class DatabaseConfig(
    val path: String = error("Please set the database path configuration (database.path)")
)

fun persistenceModule(configuration: Configuration): Module {
    val databaseConfig = configuration.extract("database") ?: DatabaseConfig()
    val url = "jdbc:sqlite:${databaseConfig.path}"
    val hikariConfig = HikariConfig().apply {
        driverClassName = "org.sqlite.JDBC"
        jdbcUrl = url
    }
    val datasource = HikariDataSource(hikariConfig)

    return module {
        single<DataSource> { datasource }

        single {
            Database.connect(get<DataSource>()).apply {
                transactionManager.defaultIsolationLevel = Connection.TRANSACTION_READ_UNCOMMITTED
                transaction(this) {
                    SchemaUtils.createMissingTablesAndColumns(
                        AbfiMonitorTable,
                        AltDaemonMonitorTable,
                        ExplorerMonitorTable,
                        GlobalVariableTable,
                        MinerMonitorTable,
                        NodeCoreMonitorTable
                    )
                }
            }
        }

        single { AbfiMonitorRepository(get()) }
        single { AltDaemonMonitorRepository(get()) }
        single { ExplorerMonitorRepository(get()) }
        single { GlobalVariableRepository(get()) }
        single { MinerMonitorRepository(get()) }
        single { NodeCoreMonitorRepository(get()) }
    }
}
