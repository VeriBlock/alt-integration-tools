package altchain.network.monitor.tool.util

import altchain.network.monitor.tool.api.AuthConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.Json
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.http.ContentType
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.auth.providers.BasicAuthCredentials
import io.ktor.client.features.auth.providers.basic

fun createHttpClient(
    authConfig: AuthConfig? = null,
    connectionTimeout: Int = 20_000
): HttpClient = HttpClient(Apache) {
    Json {
        serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
        accept(ContentType.Any)
    }
    if (authConfig != null) {
        Auth {
            basic {
                credentials {
                    BasicAuthCredentials(authConfig.username, authConfig.password)
                }
            }
        }
    }
    engine {
        socketTimeout = connectionTimeout
        connectTimeout = connectionTimeout
        connectionRequestTimeout = connectionTimeout * 2
    }
    // We will handle error responses manually as we'll be calling a RPC service's API
    expectSuccess = false
}

