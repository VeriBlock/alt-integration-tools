package altchain.network.monitor.tool.api

import altchain.network.monitor.tool.api.controller.NetworkStatus
import org.koin.core.module.Module
import org.koin.dsl.module
import org.veriblock.core.utilities.Configuration

class ApiConfig(
    val port: Int = 8080,
    val auth: AuthConfig? = null
)

class AuthConfig (
    val username: String,
    val password: String
)

fun apiModule(configuration: Configuration): Module {
    val apiConfig = configuration.extract<ApiConfig>("api") ?: ApiConfig()

    return module {
        single { apiConfig }
        single { NetworkStatus(get(), get(), get(), get(), get(), get(), get()) }

        single {
            ApiService(
                get(),
                listOf(
                    get<NetworkStatus>()
                )
            )
        }
    }
}
