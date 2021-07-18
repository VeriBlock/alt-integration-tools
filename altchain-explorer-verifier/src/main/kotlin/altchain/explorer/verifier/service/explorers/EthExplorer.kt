package altchain.explorer.verifier.service.explorers

import altchain.explorer.verifier.ExplorerConfig
import altchain.explorer.verifier.util.equalsIgnoreCase
import altchain.explorer.verifier.util.indexOfFirstOrNull
import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.html.HtmlTable

class EthExplorer : Explorer {
    override val type: ExplorerType = ExplorerType.ETH

    override suspend fun getExplorerState(explorerConfig: ExplorerConfig): Set<BlockInfo> {
        WebClient(BrowserVersion.FIREFOX_78).use { webClient ->
            webClient.addDefaultOptions(explorerConfig.authUser, explorerConfig.authPassword)
            webClient.options.isJavaScriptEnabled = true

            val builtEthExplorerUrl = when {
                explorerConfig.url.contains("/#/?pagesize=") -> explorerConfig.url
                explorerConfig.url.endsWith("/#/") -> "${explorerConfig.url}?pagesize=${explorerConfig.blockCount}"
                else -> "${explorerConfig.url}/#/?pagesize=${explorerConfig.blockCount}"
            }

            val ethExplorerPage = webClient.getPage<HtmlPage>(builtEthExplorerUrl)
            webClient.waitForBackgroundJavaScript((explorerConfig.loadDelay * 1000).toLong())
            if (!ethExplorerPage.asNormalizedText().contains("Etherparty", true)) {
                error("$builtEthExplorerUrl is not a valid ETH explorer")
            }

            val table = ethExplorerPage.getByXPath<HtmlTable>("//table[@class='table table-striped']")[0]
                ?: error("Unable to find the main table")
            val header = table.rows.first().cells.mapIndexed { _, cell ->
                cell.asNormalizedText()
            }
            val indexOfHeight = header.indexOfFirstOrNull { it.equalsIgnoreCase("Block") }
                ?: error("Unable to find the index of the field Block")
            val indexOfNATV = header.indexOfFirstOrNull { it.equalsIgnoreCase("ATV") }
                ?: error("Unable to find the index of the field ATV")
            val indexOfNVTB = header.indexOfFirstOrNull { it.equalsIgnoreCase("VTB") }
                ?: error("Unable to find the index of the field VTB")
            val indexOfNVBK = header.indexOfFirstOrNull { it.equalsIgnoreCase("VBK") }
                ?: error("Unable to find the index of the field VBK")

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