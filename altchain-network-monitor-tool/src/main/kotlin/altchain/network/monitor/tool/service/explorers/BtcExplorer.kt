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

    override suspend fun getMonitor(networkId: String, id: String, config: ExplorerConfig): ExplorerMonitor {
        httpClients.getOrPut("$networkId/$id") { WebClient(BrowserVersion.FIREFOX_78).also { webClient ->
            webClient.addDefaultOptions(config.auth)
            webClient.options.isJavaScriptEnabled = false
            logger.info { "($networkId/$id) Creating http client..." }
        } }.also { webClient ->
            val builtBtcExplorerUrl = when {
                config.url.contains("blocks?limit=") -> config.url
                config.url.endsWith("/") -> "${config.url}blocks?limit=${config.blockCount}"
                else -> "${config.url}/blocks?limit=${config.blockCount}"
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

            return blockInfo.toExplorerMonitor(config)
        }
    }
}