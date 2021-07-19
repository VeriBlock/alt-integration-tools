package altchain.explorer.verifier.service

import altchain.explorer.verifier.ExplorerConfig
import altchain.explorer.verifier.service.explorers.Explorer
import altchain.explorer.verifier.util.equalsIgnoreCase

class ExplorerService(
    private val explorers: Set<Explorer>
) {
    suspend fun getExplorerState(explorerConfig: ExplorerConfig) = explorers.firstOrNull {
        it.type.name.equalsIgnoreCase(explorerConfig.type)
    }?.getExplorerState(explorerConfig)
        ?: error("${explorerConfig.type} explorer is not implemented yet")
}