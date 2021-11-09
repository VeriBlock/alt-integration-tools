package altchain.network.monitor.tool.service

import altchain.network.monitor.tool.ExplorerConfig
import altchain.network.monitor.tool.service.explorers.Explorer

class ExplorerService(
    private val explorers: Set<Explorer>
) {
    suspend fun getExplorerState(networkId: String, explorerId: String, explorerConfig: ExplorerConfig) = explorers.firstOrNull {
        it.type == explorerConfig.type
    }?.getExplorerState(networkId, explorerId, explorerConfig)
        ?: error("${explorerConfig.type} explorer is not implemented yet")
}