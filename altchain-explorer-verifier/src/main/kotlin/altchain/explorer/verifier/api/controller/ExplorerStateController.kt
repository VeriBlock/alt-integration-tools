package altchain.explorer.verifier.api.controller

import altchain.explorer.verifier.api.NotFoundException
import altchain.explorer.verifier.api.toExplorerStateResponse
import altchain.explorer.verifier.api.toExplorerStateSummariesResponse
import altchain.explorer.verifier.persistence.ExplorerStateRepository
import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.delete
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route

class ExplorerStateController(
    private val explorerStateRepository: ExplorerStateRepository
) : ApiController {

    @Path("latests")
    class Latests

    @Path("{configName}/latest")
    data class LatestByConfigName(
        @PathParam("Config Name") val configName: String
    )

    @Path("{configName}")
    data class ByConfigName(
        @PathParam("Config Name") val configName: String
    )

    override fun NormalOpenAPIRoute.registerApi() = route("explorer-state") {
        get<Latests, ExplorerStateSummariesResponse>(
            info("Get the latest explorer state for all the explorers")
        ) {
            val states = explorerStateRepository.getLatestExplorerStates().toExplorerStateSummariesResponse()
            respond(states)
        }
        get<LatestByConfigName, ExplorerStateResponse>(
            info("Get the latest explorer state by config name")
        ) { location ->
            val state = explorerStateRepository.getLatestExplorerState(location.configName)
                ?: throw NotFoundException("The explorer ${location.configName} doesn't exists")
            respond(state.toExplorerStateResponse())
        }
        delete<ByConfigName, Unit>(
            info("Delete all the entries by config name")
        ) { location ->
            val count = explorerStateRepository.delete(location.configName)
            if (count == 0) {
                throw NotFoundException("The explorer ${location.configName} doesn't exists")
            }
            respond(Unit)
        }
    }
}