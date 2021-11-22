package altchain.network.monitor.tool.service.vbfi

import altchain.network.monitor.tool.VbfiConfig
import altchain.network.monitor.tool.persistence.tables.VbfiMonitor
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

    suspend fun getVbfiMonitor(networkId: String, vbfiId: String, vbfiConfig: VbfiConfig): VbfiMonitor {
        httpClients.getOrPut("$networkId/$vbfiId") { createHttpClient(vbfiConfig.auth).also {
            logger.info { "($networkId/$vbfiId) Creating http client..." }
        } }.also { httpClient ->
            val chainsDto: ChainsDto = httpClient.get("${vbfiConfig.apiUrl}/api/chains")
            val explorerDto: ExplorerDto = httpClient.get(vbfiConfig.explorerApiUrl)

            val vbfiLastBlock = chainsDto.best.blocks.maxOf { it.height }
            val explorerLastBlock = explorerDto.lastBlock.height
            val blockDifference = abs(vbfiLastBlock - explorerLastBlock)
            val isSynchronized = vbfiLastBlock > 0 && blockDifference <= vbfiConfig.maxBlockDifference

            return VbfiMonitor(
                vbfiVersion = "Unknown",
                lastBlockHeight = vbfiLastBlock,
                lastExplorerBlockHeight = explorerLastBlock,
                isSynchronized = isSynchronized,
                addedAt = now()
            )
        }
    }
}