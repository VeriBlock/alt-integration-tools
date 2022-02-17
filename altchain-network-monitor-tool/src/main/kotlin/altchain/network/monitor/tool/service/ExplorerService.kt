package altchain.network.monitor.tool.service

import altchain.network.monitor.tool.ExplorerConfig
import altchain.network.monitor.tool.service.explorers.Explorer

class ExplorerService(
    private val explorers: Set<Explorer>
) {
    suspend fun getExplorerState(networkId: String, id: String, config: ExplorerConfig) = explorers.firstOrNull {
        it.type == config.type
    }?.getMonitor(networkId, id, config)
        ?: error("${config.type} explorer is not implemented yet")
}