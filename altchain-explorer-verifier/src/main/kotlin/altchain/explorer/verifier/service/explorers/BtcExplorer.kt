package altchain.explorer.verifier.service.explorers

import altchain.explorer.verifier.ExplorerConfig
import altchain.explorer.verifier.util.equalsIgnoreCase
import altchain.explorer.verifier.util.indexOfFirstOrNull
import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.html.HtmlTable

class BtcExplorer : Explorer {
    override val type: ExplorerType = ExplorerType.BTC

    override suspend fun getExplorerState(explorerConfig: ExplorerConfig): Set<BlockInfo> {
        WebClient(BrowserVersion.FIREFOX_78).use { webClient ->
            webClient.addDefaultOptions(explorerConfig.authUser, explorerConfig.authPassword)
            webClient.options.isJavaScriptEnabled = false

            val builtBtcExplorerUrl = when {
                explorerConfig.url.contains("blocks?limit=") -> explorerConfig.url
                explorerConfig.url.endsWith("/") -> "${explorerConfig.url}blocks?limit=${explorerConfig.blockCount}"
                else -> "${explorerConfig.url}/blocks?limit=${explorerConfig.blockCount}"
            }

            val btcExplorerPage = webClient.getPage<HtmlPage>(builtBtcExplorerUrl)
            if (!btcExplorerPage.asNormalizedText().contains("janoside", true)) {
                error("$builtBtcExplorerUrl is not a valid BTC explorer")
            }

            val table = btcExplorerPage.getByXPath<HtmlTable>("//table[@class='table table-striped mb-0']")[0]
                ?: error("Unable to find the main table")
            val header = table.rows.first().cells.mapIndexed { _, cell ->
                cell.asNormalizedText()
            }
            val indexOfHeight = header.indexOfFirstOrNull { it.equalsIgnoreCase("Height") }
                ?: error("Unable to find the index of the field Height")
            val indexOfNATV = header.indexOfFirstOrNull { it.equalsIgnoreCase("N(ATV)") }
                ?: error("Unable to find the index of the field N(ATV)")
            val indexOfNVTB = header.indexOfFirstOrNull { it.equalsIgnoreCase("N(VTB)") }
                ?: error("Unable to find the index of the field N(VTB)")
            val indexOfNVBK = header.indexOfFirstOrNull { it.equalsIgnoreCase("N(VBK)") }
                ?: error("Unable to find the index of the field N(VBK)")

            return table.rows.drop(1).asSequence().map {
                BlockInfo(
                    height = it.cells[indexOfHeight].toInt(),
                    atvs = it.cells[indexOfNATV].toInt(),
                    vtbs = it.cells[indexOfNVTB].toInt(),
                    vbks = it.cells[indexOfNVBK].toInt()
                )
            }.toSet()
        }
    }
}