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

class EthExplorer : Explorer {
    override val type: ExplorerType = ExplorerType.ETH

    private val httpClients: MutableMap<String, WebClient> by lazy {
        HashMap()
    }

    override suspend fun getExplorerState(networkId: String, explorerId: String, explorerConfig: ExplorerConfig): ExplorerMonitor {
        httpClients.getOrPut("$networkId/$explorerId") { WebClient(BrowserVersion.FIREFOX_78).also { webClient ->
            webClient.addDefaultOptions(explorerConfig.auth)
            webClient.options.isJavaScriptEnabled = true
            logger.info { "($networkId/$explorerId) Creating http client..." }
        } }.also { webClient ->
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

            val blockInfo = table.rows.drop(1).asSequence().map {
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