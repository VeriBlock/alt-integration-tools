package altchain.explorer.verifier.api.auth

import altchain.explorer.verifier.api.Auth
import altchain.explorer.verifier.api.BadPrincipalException
import com.papsign.ktor.openapigen.APIException
import com.papsign.ktor.openapigen.model.Described
import com.papsign.ktor.openapigen.model.security.APIKeyLocation
import com.papsign.ktor.openapigen.model.security.HttpSecurityScheme
import com.papsign.ktor.openapigen.model.security.SecuritySchemeModel
import com.papsign.ktor.openapigen.model.security.SecuritySchemeType
import com.papsign.ktor.openapigen.modules.providers.AuthProvider
import com.papsign.ktor.openapigen.route.path.auth.OpenAPIAuthenticatedRoute
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.throws
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.util.pipeline.*

const val basicAuthName = "basicAuth"


inline fun<T> NormalOpenAPIRoute.auth(provider: AuthProvider<T>, crossinline route: OpenAPIAuthenticatedRoute<T>.()->Unit = {}): OpenAPIAuthenticatedRoute<T> {
    return provider.apply(this).apply {
        route()
    }
}

class BasicProvider(
    private val auth: Auth?
) : AuthProvider<UserIdPrincipal> {
    override suspend fun getAuth(pipeline: PipelineContext<Unit, ApplicationCall>): UserIdPrincipal {
        return pipeline.context.authentication.principal() ?: throw RuntimeException("No BasicAuthPrincipal")
    }

    override fun apply(route: NormalOpenAPIRoute): OpenAPIAuthenticatedRoute<UserIdPrincipal> =
        OpenAPIAuthenticatedRoute(
            if (auth != null) {
                route.ktorRoute.authenticate(basicAuthName) {}
            } else {
                route.ktorRoute
            },
            route.provider.child(),
            this
        ).throws(APIException.apiException<BadPrincipalException>(HttpStatusCode.Unauthorized))

    override val security: Iterable<Iterable<AuthProvider.Security<*>>> =
        listOf(
            listOf(
                AuthProvider.Security(
                    SecuritySchemeModel(
                        SecuritySchemeType.http,
                        basicAuthName,
                        APIKeyLocation.header,
                        HttpSecurityScheme.basic,
                        null,
                        null
                    ),
                    emptyList<Scopes>()
                )
            )
        )
}

enum class Scopes(override val description: String) : Described
