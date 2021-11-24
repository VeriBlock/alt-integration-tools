package altchain.network.monitor.tool.service.abfi

import kotlinx.serialization.Serializable

@Serializable
data class BlockSummaryDto(
    val height: Int,
    val hash: String
)

@Serializable
data class PingDto(
    val version: String,
    val siChainName: String,
    val healthy: Boolean,
    val error: String?,
    val lastBlock: PingBlockDto?,
    val oldestBlock: PingBlockDto?,
    val lastEndorsedBlock: PingBlockDto?,
    val oldestEndorsedBlock: PingBlockDto?,
    val lastVerifiedBlock: PingBlockDto?,
    val oldestVerifiedBlock: PingBlockDto?,
    val lastFinalizedBlock: PingBlockDto?,
    val oldestFinalizedBlock: PingBlockDto?,
    val lastFinalizedBlockBtc: PingBlockDto?,
    val oldestFinalizedBlockBtc: PingBlockDto?,
)

@Serializable
data class PingBlockDto(
    val height: Int,
    val hash: String,
    val known: Boolean,
    val popVerified: Boolean,
    val verifiedIn: BlockSummaryDto?,
    val endorsedIn: BlockSummaryDto?,
    val spFinality: Int,
    val bitcoinFinality: Int,
    val isAttackInProgress: Boolean
)