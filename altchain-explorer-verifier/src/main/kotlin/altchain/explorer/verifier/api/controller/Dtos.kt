package altchain.explorer.verifier.api.controller

import com.papsign.ktor.openapigen.annotations.Response
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Response
@Serializable
data class ExplorerStateSummariesResponse(
    val states: Set<ExplorerStateResponse>
)

@Response
@Serializable
data class ExplorerStateResponse(
    val configName: String,
    val url: String,
    val blockCount: Int,
    val atvCount: Int,
    val vtbCount: Int,
    val vbkCount: Int,
    val atvBlocks: Set<Int>,
    val vtbBlocks: Set<Int>,
    val vbkBlocks: Set<Int>,
    val isOk: Boolean,
    val addedAt: Instant
)