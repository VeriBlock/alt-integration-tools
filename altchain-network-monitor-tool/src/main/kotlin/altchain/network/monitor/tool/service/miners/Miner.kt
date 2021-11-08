package altchain.network.monitor.tool.service.miners

import altchain.network.monitor.tool.MinerConfig
import altchain.network.monitor.tool.persistence.tables.MinerMonitor
import altchain.network.monitor.tool.persistence.tables.MinerType

interface Miner {
    val type: MinerType
    suspend fun getMinerMonitor(networkId: String, minerId: String, minerConfig: MinerConfig): MinerMonitor
}