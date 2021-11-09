package altchain.network.monitor.tool.api.controller;

import com.papsign.ktor.openapigen.route.path.auth.OpenAPIAuthenticatedRoute
import io.ktor.auth.UserIdPrincipal

interface ApiController {
    fun OpenAPIAuthenticatedRoute<UserIdPrincipal>.registerApi()
}
