package altchain.network.monitor.tool

import altchain.network.monitor.tool.api.AuthConfig
import altchain.network.monitor.tool.persistence.tables.MinerType
import altchain.network.monitor.tool.service.explorers.ExplorerType
import altchain.network.monitor.tool.util.createLogger
import java.io.File
import org.koin.core.module.Module
import org.koin.dsl.module
import org.veriblock.core.utilities.Configuration

private val logger = createLogger {}

data class CleanupConfig(
    val runDelay: Long = 0,
    val hoursAgo: Int = 120
)

data class NetworkConfig(
    val checkDelay: Long = 10,
    val minPercentageHealthyNodeCores: Int = 90,
    val minPercentageHealthyAltDaemons: Int = 75,
    val minPercentageHealthyExplorers: Int = 100,
    val minPercentageHealthyAbfis: Int = 100,
    val minPercentageHealthyVbfis: Int = 100,
    val minPercentageHealthyVpms: Int = 90,
    val maxPercentageNotHealthyVpmOperations: Int = 10,
    val minPercentageHealthyApms: Int = 80,
    val maxPercentageNotHealthyApmOperations: Int = 20,
    val maxHealthyByTime: Int = 10,
    val miners: Map<String, MinerConfig> = emptyMap(),
    val nodecores: Map<String, NodecoreConfig> = emptyMap(),
    val altDaemons: Map<String, AltDaemonConfig> = emptyMap(),
    val abfis: Map<String, AbfiConfig> = emptyMap(),
    val vbfis: Map<String, VbfiConfig> = emptyMap(),
    val explorers: Map<String, ExplorerConfig> = emptyMap()
)

data class MinerConfig(
    val apiUrl: String = "",
    val type: MinerType = MinerType.VPM,
    val altchainKey: String? = null,
    val auth: AuthConfig? = null
)

data class NodecoreConfig(
    val host: String = "",
    val port: Int = 10500,
    val ssl: Boolean = false,
    val password: String? = null
)

data class AltDaemonConfig(
    val siKey: String = ""
)

data class AbfiConfig(
    val apiUrl: String = "",
    val prefix: String = "",
    val auth: AuthConfig? = null,
    val siKey: String = "",
    val maxBlockDifference: Int = 100
)

data class VbfiConfig(
    val apiUrl: String = "",
    val auth: AuthConfig? = null,
    val explorerApiUrl: String = "",
    val maxBlockDifference: Int = 40
)

data class ExplorerConfig(
    val url: String = "",
    val blockCount: Int = 50,
    val loadDelay: Int = 40,
    val checkDelay: Long = 10,
    val type: ExplorerType = ExplorerType.BTC,
    val auth: AuthConfig? = null
)

fun configModule(configuration: Configuration): Module {
    val cleanupConfig = configuration.extract("cleanup") ?: CleanupConfig()
    val networkConfigFolder = File("./network-configs")
    val networkRawConfigurations = if (networkConfigFolder.exists()) {
        networkConfigFolder.listFiles().toList().associate {
            val configuration = Configuration(
                configFilePath = it.path
            )
            val name = it.name.replace(".conf", "")
            name to configuration
        }
    } else {
        emptyMap()
    }

    val networkConfigurations = networkRawConfigurations.entries.associate { (name, configuration) ->
        val networkConfig = configuration.extract<NetworkConfig>(name) ?: NetworkConfig()
        logger.info {
            "Loaded $name chain configuration with: NodeCores (${networkConfig.nodecores.size}), Miners (${networkConfig.miners.size}), AltDaemons (${networkConfig.altDaemons.size}), Abfis (${networkConfig.abfis.size}), Explorers (${networkConfig.explorers.size})"
        }
        name to networkConfig
    }

    return module {
        single { networkRawConfigurations.values.toList() }
        single { configuration }
        single { networkConfigurations }
        single { cleanupConfig }
    }
}
