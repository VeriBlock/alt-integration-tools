package altchain.explorer.verifier.api

import altchain.explorer.verifier.api.controller.ExplorerStateController
import altchain.explorer.verifier.util.Configuration
import org.koin.core.module.Module
import org.koin.dsl.module

class ApiConfig(
    val port: Int = 8080,
    val auth: Auth? = null
)

class Auth (
    val username: String,
    val password: String
)

fun apiModule(configuration: Configuration): Module {
    val apiConfig = configuration.extract<ApiConfig>("api") ?: ApiConfig()

    return module {
        single { apiConfig }
        single { ExplorerStateController(get()) }

        single {
            ApiService(
                get(),
                listOf(
                    get<ExplorerStateController>(),
                )
            )
        }
    }
}
