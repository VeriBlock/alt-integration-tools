package altchain.explorer.verifier.api.auth

import altchain.explorer.verifier.api.ApiConfig
import io.ktor.application.*
import io.ktor.auth.*

fun Application.installAuth(
    config: ApiConfig
) {
    install(Authentication) {
        basic(basicAuthName) {
            validate { credentials ->
                if (config.auth != null) {
                    if (credentials.name == config.auth.username && credentials.password == config.auth.password) {
                        UserIdPrincipal(credentials.name)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        }
    }
}