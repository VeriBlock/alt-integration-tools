package altchain.explorer.verifier.service.explorers

import altchain.explorer.verifier.ExplorerConfig
import altchain.explorer.verifier.api.Auth
import altchain.explorer.verifier.util.toBase64
import com.gargoylesoftware.htmlunit.IncorrectnessListener
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlTableCell

interface Explorer {
    val type: ExplorerType
    suspend fun getExplorerState(explorerConfig: ExplorerConfig): Set<BlockInfo>
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

fun WebClient.addDefaultOptions(auth: Auth?) {
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