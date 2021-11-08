package altchain.network.monitor.tool.service.miners

import com.papsign.ktor.openapigen.annotations.Response
import kotlinx.serialization.Serializable

@Serializable
data class VersionResponse(
    val name: String
)

@Serializable
data class DiagnosticInformation(
    val information: List<String>
)

@Serializable
data class OperationSummaryListResponse(
    val operations: List<OperationSummaryResponse>,
    val totalCount: Int? = null
)

@Serializable
data class OperationSummaryResponse(
    val operationId: String,
    val chain: String,
    val endorsedBlockHeight: Int?,
    val state: String,
    val task: String,
    val createdAt: String
)

@Serializable
data class AutoMineConfigResponse(
    val round1: Boolean?,
    val round2: Boolean?,
    val round3: Boolean?,
    val round4: Boolean?
)

@Serializable
class VpmOperationSummaryListResponse(
    val operations: List<VpmOperationSummaryResponse>
)

@Serializable
class VpmOperationSummaryResponse(
    val operationId: String,
    val endorsedBlockNumber: Int,
    val state: String,
    val action: String,
    val createdAt: String
)