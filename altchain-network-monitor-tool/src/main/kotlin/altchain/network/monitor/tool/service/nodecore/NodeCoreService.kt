package altchain.network.monitor.tool.service.nodecore

import altchain.network.monitor.tool.NodecoreConfig
import altchain.network.monitor.tool.persistence.tables.NodeCoreMonitor
import altchain.network.monitor.tool.util.createHttpClient
import altchain.network.monitor.tool.util.createLogger
import altchain.network.monitor.tool.util.now
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.veriblock.sdk.models.StateInfo
import kotlin.math.abs

val defaultStateInfo = StateInfo()

private val logger = createLogger {}

private val json = Json {
    ignoreUnknownKeys = true
}

class NodeCoreService {
    private val httpClients: MutableMap<String, HttpClient> by lazy {
        HashMap()
    }

    suspend fun getNodeCoreMonitor(networkId: String, nodecoreId: String, nodeCoreConfig: NodecoreConfig): NodeCoreMonitor {
        httpClients.getOrPut("$networkId/$nodecoreId") { createHttpClient().also {
            logger.info { "($networkId/$nodecoreId) Creating http client..." }
        } }.also { httpClient ->
            val response: RpcResponse = httpClient.post("http://${nodeCoreConfig.host}:${nodeCoreConfig.port}/api") {
                // Since jsonBody is a string, we have to specify it is Json content type
                body = TextContent("""{"jsonrpc": "2.0", "method": "getstateinfo", "params": {}, "id": 1}""", contentType = ContentType.Application.Json)
            }

            val nodeCoreState: NodeCoreState = json.decodeFromString(response.result.toString())
            val blockDifference = abs(nodeCoreState.networkHeight - nodeCoreState.localBlockchainHeight)
            val isSynchronized = nodeCoreState.networkHeight > 0 && blockDifference < 4

            return NodeCoreMonitor(
                nodecoreVersion = nodeCoreState.programVersion,
                localHeight = nodeCoreState.localBlockchainHeight,
                networkHeight = nodeCoreState.networkHeight,
                isSynchronized = isSynchronized,
                addedAt = now()
            )
        }
    }
}