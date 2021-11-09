package altchain.network.monitor.tool.service.nodecore

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class RpcResponse(
    val result: JsonElement,
    val error: RpcError? = null
)

@Serializable
data class RpcError(
    val code: Int,
    val message: String
)

@Serializable
data class NodeCoreState(
    val blockchainState: StateReport,
    val operatingState: StateReport,
    val networkState: StateReport,
    val connectedPeerCount: Int,
    val networkHeight: Int,
    val localBlockchainHeight: Int,
    val success: Boolean,
    val networkVersion: String,
    val dataDirectory: String,
    val programVersion: String,
    val nodecoreStarttime: Long,
    val walletCacheSyncHeight: Int
)

@Serializable
data class StateReport(
    val state: String
)
