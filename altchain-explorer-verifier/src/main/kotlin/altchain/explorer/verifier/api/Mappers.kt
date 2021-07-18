package altchain.explorer.verifier.api

import altchain.explorer.verifier.api.controller.ExplorerStateResponse
import altchain.explorer.verifier.api.controller.ExplorerStateSummariesResponse
import altchain.explorer.verifier.persistence.ExplorerState

fun ExplorerState.toExplorerStateResponse(): ExplorerStateResponse = ExplorerStateResponse(
    configName,
    url,
    blockCount,
    atvCount,
    vtbCount,
    vbkCount,
    atvBlocks,
    vtbBlocks,
    vbkBlocks,
    atvCount > 0 && vtbCount > 0 && vbkCount > 0,
    addedAt
)

fun Set<ExplorerState>.toExplorerStateSummariesResponse(): ExplorerStateSummariesResponse = ExplorerStateSummariesResponse(
    asSequence().map {
        it.toExplorerStateResponse()
    }.toSet()
)