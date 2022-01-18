package altchain.network.monitor.tool.service.miners

import kotlinx.serialization.Serializable

@Serializable
data class VersionResponse(
    val name: String
)

@Serializable
data class DiagnosticInformation(
    val information: List<String>
)