package altchain.explorer.verifier.api

import altchain.explorer.verifier.api.auth.BasicProvider
import altchain.explorer.verifier.api.auth.auth
import altchain.explorer.verifier.api.auth.installAuth
import altchain.explorer.verifier.api.controller.ApiController
import altchain.explorer.verifier.util.createLogger
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
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties

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
                    title = "Altchain Explorer Verifier"
                    description = "Altchain Explorer Verifier API"
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
                })
            }

            install(Locations)

            routing {
                get("openapi.json") {
                    call.respond(application.openAPIGen.api.serializeKt())
                }
                get {
                    call.respondRedirect("/swagger-ui/index.html?url=/openapi.json", true)
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
    fun Any?.toJsonElement(): JsonElement {
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

    fun Map<String, *>.clean(): JsonObject {
        val map = filterValues {
            when (it) {
                is Map<*, *> -> it.isNotEmpty()
                is Collection<*> -> it.isNotEmpty()
                else -> it != null
            }
        }
        return JsonObject(map.mapValues { entry -> entry.value.toJsonElement() }.filterNot { it.value == JsonNull })
    }

    fun cvt(value: Any?): JsonElement {
        return when (value) {
            is DataModel -> value.serializeKt()
            is Map<*, *> -> value.entries.associate { (key, value) -> Pair(key.toString(), cvt(value)) }.clean()
            is Iterable<*> -> JsonArray(value.mapNotNull { cvt(it) })
            else -> value.toJsonElement()
        }
    }
    return this::class.memberProperties.associateBy { it.name }.mapValues { (_, prop) ->
        cvt((prop as KProperty1<DataModel, *>).get(this))
    }.clean()
}
