package altchain.explorer.verifier.persistence

import altchain.explorer.verifier.util.Configuration
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
                        ExplorerStateTable
                    )
                }
            }
        }

        single { ExplorerStateRepository(get()) }
    }
}
