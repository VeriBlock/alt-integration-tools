package altchain.network.monitor.tool.service

import altchain.network.monitor.tool.MinerConfig
import altchain.network.monitor.tool.persistence.tables.MinerMonitor
import altchain.network.monitor.tool.service.miners.Miner

class MinerService(
    private val miners: Set<Miner>
) {
    suspend fun getMinerMonitor(networkId: String, minerId: String, minerConfig: MinerConfig): MinerMonitor = miners.firstOrNull {
        it.type == minerConfig.type
    }?.getMinerMonitor(networkId, minerId, minerConfig)
        ?: error("${minerConfig.type} miner is not implemented yet")
}