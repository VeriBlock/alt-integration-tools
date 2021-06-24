package altchain.explorer.verifier

import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.IncorrectnessListener
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.html.HtmlTable
import mu.KotlinLogging
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.system.exitProcess

private val logger = createLogger {}

class AltchainExplorerVerifier(
    private val blockCount: Int,
    private val explorerUrl: String,
    private val explorerType: ExplorerType,
    private val authUser: String? = null,
    private val authPassword: String? = null,
    private val loadDelay: Int
) {
    private val collectedBlockData = ArrayList<BlockData>()

    data class BlockData (
        val height: String? = null,
        val nAtv: String? = null,
        val nVtb: String? = null,
        val nVbk: String? = null
    )

    fun doCheck() {
        WebClient(BrowserVersion.FIREFOX_78).use { webClient ->
            webClient.addDefaultOptions(authUser, authPassword)

            if (explorerType == ExplorerType.BTC) {
                webClient.options.isJavaScriptEnabled = false

                val builtBtcExplorerUrl = when {
                    explorerUrl.contains("blocks?limit=") -> explorerUrl
                    explorerUrl.endsWith("/") -> "${explorerUrl}blocks?limit=$blockCount"
                    else -> "${explorerUrl}/blocks?limit=$blockCount"
                }

                val btcExplorerPage = webClient.getPage<HtmlPage>(builtBtcExplorerUrl)
                if (!btcExplorerPage.asNormalizedText().contains("janoside", true)) {
                    logger.warn { "$builtBtcExplorerUrl is not a valid BTC explorer" }
                    exitProcess(1)
                }
                logger.info { "Checking ${btcExplorerPage.url}..." }

                val table = btcExplorerPage.getByXPath<HtmlTable>("//table[@class='table table-striped mb-0']")[0]
                    ?: error("Unable to find the main table")
                val header = table.rows.first().cells.mapIndexed { _, cell ->
                    cell.asNormalizedText()
                }
                val indexOfHeight = header.indexOfFirstOrNull { it.equals("Height", true) }
                    ?: error("Unable to find the index of the field Height")
                val indexOfNATV = header.indexOfFirstOrNull { it.equals("N(ATV)", true) }
                    ?: error("Unable to find the index of the field N(ATV)")
                val indexOfNVTB = header.indexOfFirstOrNull { it.equals("N(VTB)", true) }
                    ?: error("Unable to find the index of the field N(VTB)")
                val indexOfNVBK = header.indexOfFirstOrNull { it.equals("N(VBK)", true) }
                    ?: error("Unable to find the index of the field N(VBK)")
                for (row in table.rows.drop(1)) {
                    collectedBlockData.add(
                        BlockData(
                            height = row.cells[indexOfHeight].asNormalizedText(),
                            nAtv = row.cells[indexOfNATV].asNormalizedText(),
                            nVtb = row.cells[indexOfNVTB].asNormalizedText(),
                            nVbk = row.cells[indexOfNVBK].asNormalizedText()
                        )
                    )
                }
            } else {
                webClient.options.isJavaScriptEnabled = true

                val builtEthExplorerUrl = when {
                    explorerUrl.contains("/#/?pagesize=") -> explorerUrl
                    explorerUrl.endsWith("/#/") -> "${explorerUrl}?pagesize=$blockCount"
                    else -> "${explorerUrl}/#/?pagesize=$blockCount"
                }

                val ethExplorerPage = webClient.getPage<HtmlPage>(builtEthExplorerUrl)
                webClient.waitForBackgroundJavaScript((loadDelay * 1000).toLong())
                if (!ethExplorerPage.asNormalizedText().contains("Etherparty", true)) {
                    logger.warn { "$builtEthExplorerUrl is not a valid ETH explorer" }
                    exitProcess(1)
                }
                logger.info { "Checking ${ethExplorerPage.url}..." }

                val table = ethExplorerPage.getByXPath<HtmlTable>("//table[@class='table table-striped']")[0]
                    ?: error("Unable to find the main table")
                val header = table.rows.first().cells.mapIndexed { _, cell ->
                    cell.asNormalizedText()
                }
                val indexOfHeight = header.indexOfFirstOrNull { it.equals("Block", true) }
                    ?: error("Unable to find the index of the field Block")
                val indexOfNATV = header.indexOfFirstOrNull { it.equals("ATV", true) }
                    ?: error("Unable to find the index of the field ATV")
                val indexOfNVTB = header.indexOfFirstOrNull { it.equals("VTB", true) }
                    ?: error("Unable to find the index of the field VTB")
                val indexOfNVBK = header.indexOfFirstOrNull { it.equals("VBK", true) }
                    ?: error("Unable to find the index of the field VBK")
                for (row in table.rows.drop(1)) {
                    collectedBlockData.add(
                        BlockData(
                            height = row.cells[indexOfHeight].asNormalizedText(),
                            nAtv = row.cells[indexOfNATV].asNormalizedText(),
                            nVtb = row.cells[indexOfNVTB].asNormalizedText(),
                            nVbk = row.cells[indexOfNVBK].asNormalizedText()
                        )
                    )
                }
            }
            if (collectedBlockData.size != 0) {
                val nAtvCount = collectedBlockData.count { it.nAtv?.toIntOrNull() ?: 0 > 0 }
                val nVtbCount = collectedBlockData.count { it.nVtb?.toIntOrNull() ?: 0 > 0 }
                val nVbkCount = collectedBlockData.count { it.nVbk?.toIntOrNull() ?: 0 > 0 }
                val isOk = nAtvCount > 0 && nVtbCount > 0 && nVbkCount > 0
                logger.info { "${if (isOk) "+" else "-"} Checked the last ${collectedBlockData.size} blocks (out of $blockCount requested) and found $nAtvCount with at least one ATV, $nVtbCount with at least one VTB and $nVbkCount with at least one VBK" }
            } else {
                logger.info { "Failed to parse the blocks" }
            }
        }
    }
}

fun main(args: Array<String>) {
    Logger.getLogger("com.gargoylesoftware").level = Level.OFF
    val options = Options().also {
        it.addOption(
            Option.builder("blockCount")
                .argName("blockCount")
                .hasArg()
                .required(false)
                .desc("The amount of blocks to perform the query")
                .longOpt("blockCount")
                .build()
        )
        it.addOption(
            Option.builder("explorerUrl")
                .argName("explorerUrl")
                .hasArg()
                .required(false)
                .desc("The explorer url to check")
                .longOpt("explorerUrl")
                .build()
        )
        it.addOption(
            Option.builder("explorerType")
                .argName("explorerType")
                .hasArg()
                .required(false)
                .desc("The explorer type can be BTC or ETH")
                .longOpt("explorerType")
                .build()
        )
        it.addOption(
            Option.builder("authUser")
                .argName("authUser")
                .hasArg()
                .required(false)
                .desc("The explorer auth username")
                .longOpt("authUser")
                .build()
        )
        it.addOption(
            Option.builder("authPassword")
                .argName("authPassword")
                .hasArg()
                .required(false)
                .desc("The explorer auth password")
                .longOpt("authPassword")
                .build()
        )
        it.addOption(
            Option.builder("loadDelay")
                .argName("loadDelay")
                .hasArg()
                .required(false)
                .desc("The time to wait for the page to load in seconds")
                .longOpt("loadDelay")
                .build()
        )
    }

    val parser = DefaultParser()
    val commandLine = try {
        parser.parse(options, args)
    } catch (e: ParseException) {
        error("Unable to parse the command line arguments")
    }

    AltchainExplorerVerifier(
        commandLine.getOptionValue("blockCount", "50")!!.toInt(),
        commandLine.getOptionValue("explorerUrl", "https://testnet.explore.vbtc.veriblock.org")!!,
        ExplorerType.valueOf(commandLine.getOptionValue("explorerType", "BTC")),
        commandLine.getOptionValue("authUser", null),
        commandLine.getOptionValue("authPassword", null),
        commandLine.getOptionValue("loadDelay", "40")!!.toInt()
    ).doCheck()
}

enum class ExplorerType {
    BTC,
    ETH
}

private fun createLogger(context: () -> Unit) = KotlinLogging.logger(context)

private fun String.toBase64(): String = Base64.getEncoder().encodeToString(this.toByteArray())

private fun WebClient.addDefaultOptions(authUser: String?, authPassword: String?) {
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

    if (authUser != null && authPassword != null) {
        val base64Password = "$authUser:$authPassword".toBase64()
        addRequestHeader("Authorization", "Basic $base64Password")
    }
}

private inline fun <T> List<T>.indexOfFirstOrNull(predicate: (T) -> Boolean): Int? {
    for ((index, item) in this.withIndex()) {
        if (predicate(item)) {
            return index
        }
    }
    return null
}