package altchain.network.monitor.tool.service.vbfi

import altchain.network.monitor.tool.VbfiConfig
import altchain.network.monitor.tool.persistence.tables.VbfiMonitor
import altchain.network.monitor.tool.service.abfi.PingDto
import altchain.network.monitor.tool.util.createHttpClient
import altchain.network.monitor.tool.util.createLogger
import altchain.network.monitor.tool.util.now
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlin.math.abs

private val logger = createLogger {}

class VbfiService {
    private val httpClients: MutableMap<String, HttpClient> by lazy {
        HashMap()
    }

    suspend fun getMonitor(networkId: String, id: String, config: VbfiConfig): VbfiMonitor {
        httpClients.getOrPut("$networkId/$id") { createHttpClient(config.auth).also {
            logger.info { "($networkId/$id) Creating http client..." }
        } }.also { httpClient ->
            val pingDto: PingDto = httpClient.get("${config.apiUrl}/api/ping")

            val explorerDto: ExplorerDto = httpClient.get(config.explorerApiUrl)
            val explorerLastBlock = explorerDto.lastBlock.height

            val lastBlock = pingDto.lastBlock?.height ?: 0
            val lastBlockDifference = abs(lastBlock - explorerLastBlock)
            val isLastBlockSynchronized = lastBlock > 0 && lastBlockDifference <= config.maxLastBlockDifference

            val lastBlockFinalizedBtc = pingDto.lastFinalizedBlockBtc?.height ?: 0
            val lastBlockFinalizedBtcDifference = abs(lastBlockFinalizedBtc - explorerLastBlock)
            val isLastBlockFinalizedBtcSynchronized = lastBlockFinalizedBtc > 0 && lastBlockFinalizedBtcDifference <= config.maxLastBlockFinalizedBtcDifference

            return VbfiMonitor(
                vbfiVersion = pingDto.version,
                lastBlockHeight = lastBlock,
                lastBlockFinalizedBtcHeight = lastBlockFinalizedBtc,
                lastExplorerBlockHeight = explorerLastBlock,
                isLastBlockSynchronized = isLastBlockSynchronized,
                isLastBlockFinalizedBtcSynchronized = isLastBlockFinalizedBtcSynchronized,
                addedAt = now()
            )
        }
    }
}