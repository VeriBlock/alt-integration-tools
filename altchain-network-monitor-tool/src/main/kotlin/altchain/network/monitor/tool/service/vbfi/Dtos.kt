package altchain.network.monitor.tool.service.vbfi

import kotlinx.serialization.Serializable

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