package altchain.network.monitor.tool.service

import altchain.network.monitor.tool.MinerConfig
import altchain.network.monitor.tool.persistence.tables.MinerMonitor
import altchain.network.monitor.tool.service.miners.Miner

class MinerService(
    private val miners: Set<Miner>
) {
    suspend fun getMinerMonitor(networkId: String, id: String, config: MinerConfig): MinerMonitor = miners.firstOrNull {
        it.type == config.type
    }?.getMonitor(networkId, id, config)
        ?: error("${config.type} miner is not implemented yet")
}