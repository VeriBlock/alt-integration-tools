package altchain.explorer.verifier.api

import altchain.explorer.verifier.util.createLogger
import com.papsign.ktor.openapigen.annotations.Response
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import kotlinx.serialization.Serializable

private val logger = createLogger {}

class BadRequestException(override val message: String) : RuntimeException()
class NotFoundException(override val message: String) : RuntimeException()
class InternalErrorException(override val message: String) : RuntimeException()
class ForbiddenException(override val message: String) : RuntimeException()
class UnauthorizedException(override val message: String) : RuntimeException()

fun Application.installExceptionHandling(extraConfig: StatusPages.Configuration.() -> Unit = {}) {
    install(StatusPages) {
        extraConfig()
        exception<UnauthorizedException> {
            call.respond(HttpStatusCode.Unauthorized, ApiError(it.message))
        }
        exception<ForbiddenException> {
            call.respond(HttpStatusCode.Forbidden, ApiError(it.message))
        }
        exception<BadRequestException> {
            call.respond(HttpStatusCode.BadRequest, ApiError(it.message))
        }
        exception<NotFoundException> {
            call.respond(HttpStatusCode.NotFound, ApiError(it.message))
        }
        exception<InternalErrorException> {
            logger.info { "Internal error response returned: ${it.message}" }
            call.respond(HttpStatusCode.InternalServerError, ApiError("Internal server error!"))
        }
        exception<Exception> {
            logger.warn("Unhandled exception", it)
            call.respond(HttpStatusCode.InternalServerError, ApiError("Internal server error!"))
        }
    }
}

@Response
@Serializable
data class ApiError(
    val message: String
)
