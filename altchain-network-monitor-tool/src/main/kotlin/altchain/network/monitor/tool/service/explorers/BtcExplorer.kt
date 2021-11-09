package altchain.network.monitor.tool.service.explorers

import altchain.network.monitor.tool.ExplorerConfig
import altchain.network.monitor.tool.persistence.tables.ExplorerMonitor
import altchain.network.monitor.tool.util.createLogger
import altchain.network.monitor.tool.util.equalsIgnoreCase
import altchain.network.monitor.tool.util.indexOfFirstOrNull
import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.html.HtmlTable

private val logger = createLogger {}

class BtcExplorer : Explorer {
    override val type: ExplorerType = ExplorerType.BTC

    private val httpClients: MutableMap<String, WebClient> by lazy {
        HashMap()
    }

    override suspend fun getExplorerState(networkId: String, explorerId: String, explorerConfig: ExplorerConfig): ExplorerMonitor {
        httpClients.getOrPut("$networkId/$explorerId") { WebClient(BrowserVersion.FIREFOX_78).also { webClient ->
            webClient.addDefaultOptions(explorerConfig.auth)
            webClient.options.isJavaScriptEnabled = false
            logger.info { "($networkId/$explorerId) Creating http client..." }
        } }.also { webClient ->
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

            val blockInfo =  table.rows.drop(1).asSequence().map {
                BlockInfo(
                    height = it.cells[indexOfHeight].toInt(),
                    atvs = it.cells[indexOfNATV].toInt(),
                    vtbs = it.cells[indexOfNVTB].toInt(),
                    vbks = it.cells[indexOfNVBK].toInt()
                )
            }.toSet()

            return blockInfo.toExplorerMonitor(explorerConfig)
        }
    }
}