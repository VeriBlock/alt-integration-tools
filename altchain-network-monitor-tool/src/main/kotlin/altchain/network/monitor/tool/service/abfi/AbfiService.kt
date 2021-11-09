package altchain.network.monitor.tool.service.abfi

import altchain.network.monitor.tool.AbfiConfig
import altchain.network.monitor.tool.persistence.tables.AbfiBlockInfoRecord
import altchain.network.monitor.tool.persistence.tables.AbfiBlockRecord
import altchain.network.monitor.tool.persistence.tables.AbfiBlocksRecord
import altchain.network.monitor.tool.persistence.tables.AbfiMonitor
import altchain.network.monitor.tool.util.createHttpClient
import altchain.network.monitor.tool.util.createLogger
import altchain.network.monitor.tool.util.now
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val logger = createLogger {}

class AbfiService {
    private val httpClients: MutableMap<String, HttpClient> by lazy {
        HashMap()
    }

    suspend fun getAbfiMonitor(networkId: String, abfiId: String, abfiConfig: AbfiConfig): AbfiMonitor {
        httpClients.getOrPut("$networkId/$abfiId") { createHttpClient(abfiConfig.auth).also {
            logger.info { "($networkId/$abfiId) Creating http client..." }
        } }.also { httpClient ->
            val pingDto: PingDto = httpClient.get("${abfiConfig.apiUrl}/${abfiConfig.prefix}/ping")
            val diagnostic = Json.encodeToString(pingDto)

            return AbfiMonitor(
                abfiVersion = pingDto.version,
                blockInfo = pingDto.toAbfiBlocksRecord(),
                haveLastFinalizedBlockBtc = pingDto.lastFinalizedBlockBtc != null,
                diagnostic = diagnostic,
                addedAt = now()
            )
        }
    }

    private fun PingDto.toAbfiBlocksRecord(): AbfiBlocksRecord = AbfiBlocksRecord(
        listOf(
            AbfiBlockRecord(
                name = "lastEndorsedBlock",
                blockInfo = lastEndorsedBlock.toAbfiBlockInfoRecord()
            ),
            AbfiBlockRecord(
                name = "lastVerifiedBlock",
                blockInfo = lastVerifiedBlock.toAbfiBlockInfoRecord()
            ),
            AbfiBlockRecord(
                name = "lastFinalizedBlock",
                blockInfo = lastFinalizedBlock.toAbfiBlockInfoRecord()
            ),
            AbfiBlockRecord(
                name = "lastFinalizedBlockBtc",
                blockInfo = lastFinalizedBlockBtc.toAbfiBlockInfoRecord()
            ),
            AbfiBlockRecord(
                name = "oldestEndorsedBlock",
                blockInfo = oldestEndorsedBlock.toAbfiBlockInfoRecord()
            ),
            AbfiBlockRecord(
                name = "oldestVerifiedBlock",
                blockInfo = oldestVerifiedBlock.toAbfiBlockInfoRecord()
            ),
            AbfiBlockRecord(
                name = "oldestFinalizedBlock",
                blockInfo = oldestFinalizedBlock.toAbfiBlockInfoRecord()
            ),
            AbfiBlockRecord(
                name = "oldestFinalizedBlockBtc",
                blockInfo = oldestFinalizedBlockBtc.toAbfiBlockInfoRecord()
            )
        )
    )

    private fun PingBlockDto?.toAbfiBlockInfoRecord(): AbfiBlockInfoRecord = AbfiBlockInfoRecord(
        height = this?.height ?: -1,
        spFinality = this?.spFinality ?: -1,
        bitcoinFinality = this?.bitcoinFinality ?: -1,
        endorsedInHeight = this?.endorsedIn?.height ?: -1,
        verifiedInHeight =  this?.verifiedIn?.height ?: -1
    )
}