package altchain.network.monitor.tool.service.explorers

import altchain.network.monitor.tool.ExplorerConfig
import altchain.network.monitor.tool.api.AuthConfig
import altchain.network.monitor.tool.persistence.tables.ExplorerMonitor
import altchain.network.monitor.tool.util.now
import altchain.network.monitor.tool.util.toBase64
import com.gargoylesoftware.htmlunit.IncorrectnessListener
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlTableCell

interface Explorer {
    val type: ExplorerType
    suspend fun getExplorerState(networkId: String, explorerId: String, explorerConfig: ExplorerConfig): ExplorerMonitor
}

data class BlockInfo(
    val height: Int,
    val atvs: Int,
    val vtbs: Int,
    val vbks: Int
)

enum class ExplorerType {
    BTC,
    ETH
}

fun WebClient.addDefaultOptions(auth: AuthConfig?) {
    options.isThrowExceptionOnScriptError = false
    options.isThrowExceptionOnFailingStatusCode = false
    options.isPrintContentOnFailingStatusCode = false
    cssErrorHandler = SilentCssErrorHandler()
    incorrectnessListener = IncorrectnessListener { _, _ -> }
    cookieManager.isCookiesEnabled = false
    javaScriptTimeout = 600000
    options.isDownloadImages = false
    options.isGeolocationEnabled = false
    options.isAppletEnabled = false
    options.isActiveXNative = false
    options.isRedirectEnabled = false
    options.isUseInsecureSSL = true
    options.isCssEnabled = false
    options.timeout = 300000 // 5 Min timeout

    if (auth != null) {
        val base64Password = "${auth.username}:${auth.password}".toBase64()
        addRequestHeader("Authorization", "Basic $base64Password")
    }
}

fun HtmlTableCell.toInt(): Int = try {
    asNormalizedText()
        .replace(".", "")
        .replace(",", "")
        .trim()
        .toInt()
} catch(exception: NumberFormatException) {
    0
}

fun Set<BlockInfo>.toExplorerMonitor(explorerConfig: ExplorerConfig): ExplorerMonitor {
    val atvBlocks = filter { blockInfo ->
        blockInfo.atvs > 0
    }.asSequence().map { blockInfo ->
        blockInfo.height
    }.toSet()
    val vtbBlocks = filter { blockInfo ->
        blockInfo.vtbs > 0
    }.asSequence().map { blockInfo ->
        blockInfo.height
    }.toSet()
    val vbkBlocks = filter { blockInfo ->
        blockInfo.vbks > 0
    }.asSequence().map { blockInfo ->
        blockInfo.height
    }.toSet()

    return ExplorerMonitor(
        blockCount = explorerConfig.blockCount,
        atvBlocks = atvBlocks,
        vtbBlocks = vtbBlocks,
        vbkBlocks = vbkBlocks,
        atvCount = atvBlocks.size,
        vtbCount = vtbBlocks.size,
        vbkCount = vbkBlocks.size,
        addedAt = now()
    )
}