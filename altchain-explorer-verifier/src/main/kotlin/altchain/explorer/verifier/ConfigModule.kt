package altchain.explorer.verifier

import altchain.explorer.verifier.api.Auth
import altchain.explorer.verifier.util.Configuration
import org.koin.core.module.Module
import org.koin.dsl.module

data class ExplorerConfig(
   val url: String = error("Please set the explorer url configuration (explorers.explorer.url)"),
   val blockCount: Int = 50,
   val loadDelay: Int = 40,
   val checkDelay: Long = 10,
   val type: String = "BTC",
   val auth: Auth? = null
)

fun configModule(configuration: Configuration): Module {
    val explorersConfig: Map<String, ExplorerConfig> = configuration.extract("explorers") ?: emptyMap()
    return module {
        single { configuration }
        single { explorersConfig }
    }
}
