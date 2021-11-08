package altchain.network.monitor.tool.service.altchain

import altchain.network.monitor.tool.persistence.tables.AltDaemonMonitor
import altchain.network.monitor.tool.service.nodecore.defaultStateInfo
import altchain.network.monitor.tool.util.now
import org.veriblock.core.utilities.Configuration
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.sdk.alt.plugin.PluginService

class AltchainService(
    configuration: List<Configuration>
) {
    private val pluginServices = configuration.map {
        PluginService(it)
    }

    fun getPluginByKey(key: String): SecurityInheritingChain? = pluginServices.mapNotNull {
        it.getPlugins()[key]
    }.firstOrNull()

    fun load() {
        pluginServices.forEach {
            it.loadPlugins()
        }
    }

    suspend fun getBlockChainInfo(pluginKey: String): AltDaemonMonitor  {
        return getPluginByKey(pluginKey)?.let { securityInheritingChain ->
            val stateInfo = securityInheritingChain.getBlockChainInfo()
            if (stateInfo == defaultStateInfo) {
                throw error("The node is not responding")
            }

            AltDaemonMonitor(
                host = securityInheritingChain.config.host,
                localHeight = stateInfo.localBlockchainHeight,
                networkHeight = stateInfo.networkHeight,
                isSynchronized = stateInfo.isSynchronized,
                addedAt = now()
            )
        } ?: error("$pluginKey plugin is not yet implemented")
    }
}
