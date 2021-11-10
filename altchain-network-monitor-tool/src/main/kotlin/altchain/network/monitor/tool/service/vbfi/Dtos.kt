package altchain.network.monitor.tool.service.vbfi

import kotlinx.serialization.Serializable

@Serializable
data class ChainsDto(
    val best: ChainDto
)

@Serializable
data class ChainDto(
    val blocks: List<BlockDto>
)

@Serializable
data class BlockDto(
    val height: Int
)

@Serializable
data class ExplorerDto(
    val lastBlock: LastBlockDto
)

@Serializable
data class LastBlockDto(
    val hash: String,
    val height: Int,
    val time: Long
)