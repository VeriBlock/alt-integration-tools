package altchain.explorer.verifier.api

import altchain.explorer.verifier.api.controller.ExplorerStateResponse
import altchain.explorer.verifier.api.controller.ExplorerStateSummariesResponse
import altchain.explorer.verifier.persistence.ExplorerState
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import java.time.Duration

fun ExplorerState.toExplorerStateResponse(): ExplorerStateResponse {
    val isOkByBlocks = atvCount > 0 && vtbCount > 0 && vbkCount > 0
    val isOkByTime = Duration.between(addedAt.toJavaInstant(), Clock.System.now().toJavaInstant()).toMinutes() <= 30
    return ExplorerStateResponse(
        configName = configName,
        url = url,
        blockCount = blockCount,
        atvCount = atvCount,
        vtbCount = vtbCount,
        vbkCount = vbkCount,
        atvBlocks = atvBlocks,
        vtbBlocks = vtbBlocks,
        vbkBlocks = vbkBlocks,
        isOkByBlocks = isOkByBlocks,
        isOkByTime = isOkByTime,
        isOk = isOkByBlocks && isOkByTime,
        addedAt
    )
}

fun Set<ExplorerState>.toExplorerStateSummariesResponse(): ExplorerStateSummariesResponse = ExplorerStateSummariesResponse(
    asSequence().map {
        it.toExplorerStateResponse()
    }.toSet()
)
