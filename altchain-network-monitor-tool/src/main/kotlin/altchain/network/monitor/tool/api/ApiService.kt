package altchain.network.monitor.tool.api

import altchain.network.monitor.tool.api.auth.BasicProvider
import altchain.network.monitor.tool.api.auth.auth
import altchain.network.monitor.tool.api.auth.installAuth
import altchain.network.monitor.tool.api.controller.ApiController
import altchain.network.monitor.tool.service.metrics.Metrics
import altchain.network.monitor.tool.util.createLogger
import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.model.DataModel
import com.papsign.ktor.openapigen.openAPIGen
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.schema.namer.DefaultSchemaNamer
import com.papsign.ktor.openapigen.schema.namer.SchemaNamer
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.ForwardedHeaderSupport
import io.ktor.features.XForwardedHeaderSupport
import io.ktor.http.HttpHeaders
import io.ktor.locations.Locations
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.serialization.json
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.reflect.KType

private val logger = createLogger {}

class ApiService(
    private val config: ApiConfig,
    private val controllers: List<ApiController>,
) {
    private var server: ApplicationEngine? = null

    fun start() {
        if (server != null) {
            return
        }

        logger.info { "Starting HTTP API on port ${config.port}" }

        server = embeddedServer(Netty, port = config.port) {
            install(DefaultHeaders)
            install(CallLogging)
            install(CORS) {
                header(HttpHeaders.Authorization)
                anyHost()
                allowNonSimpleContentTypes = true
            }

            install(ForwardedHeaderSupport)
            install(XForwardedHeaderSupport)

            installAuth(config)

            val authProvider  = BasicProvider(
                config.auth
            )

            install(OpenAPIGen) {
                info {
                    title = "Altchain Network Monitor"
                    description = "Altchain Network Monitor API"
                    contact {
                        name = "VeriBlock"
                        email = "https://veriblock.org"
                    }
                }
                addModules(authProvider)
                replaceModule(DefaultSchemaNamer, object: SchemaNamer {
                    val regex = Regex("[A-Za-z0-9_.]+")
                    override fun get(type: KType): String {
                        return type.toString().replace(regex) { it.value.split(".").last() }.replace(Regex(">|<|, "), "_")
                    }
                })
            }

            installExceptionHandling()

            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                })
            }

            install(MicrometerMetrics) {
                registry = Metrics.registry
                meterBinders = Metrics.meterBinders
            }

            install(Locations)

            routing {
                get("openapi.json") {
                    call.respond(application.openAPIGen.api.serializeKt())
                }
                get {
                    call.respondRedirect("/swagger-ui/index.html?url=/openapi.json", true)
                }
                get("metrics") {
                    call.respond(Metrics.registry.scrape())
                }
                apiRouting {
                    auth(authProvider) {
                        for (controller in controllers) {
                            with(controller) {
                                registerApi()
                            }
                        }
                    }
                }
            }

        }.start()
    }

    fun stop() {
        server?.stop(300, 1000)
        server = null
    }
}


/**
 * Adapter method for OpenAPIGen to properly serialize its response
 */
private fun DataModel.serializeKt(): JsonElement {
    return serialize().mapValues { (_, prop) ->
        cvt(prop)
    }.toJsonObject()
}

private fun cvt(value: Any?): JsonElement {
    return when (value) {
        is DataModel -> value.serializeKt()
        is Map<*, *> -> value.entries.associate { (key, value) -> Pair(key.toString(), cvt(value)) }.toJsonObject()
        is Iterable<*> -> JsonArray(value.mapNotNull { cvt(it) })
        else -> value.toJsonElement()
    }
}

private fun Any?.toJsonElement(): JsonElement {
    return when (this) {
        is Number -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is Enum<*> -> JsonPrimitive(this.name)
        is JsonElement -> this
        else -> {
            if (this != null) {
                // if this happens, then we might have missed to add : DataModel to a custom class so it does not get serialized!
                throw IllegalStateException("The type ${this.javaClass} ($this) is unknown")
            } else {
                JsonNull
            }
        }
    }
}

private fun Map<String, *>.toJsonObject(): JsonObject {
    return JsonObject(mapValues { entry -> entry.value.toJsonElement() })
}
