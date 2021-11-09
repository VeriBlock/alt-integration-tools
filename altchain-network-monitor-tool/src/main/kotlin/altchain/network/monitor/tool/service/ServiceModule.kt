package altchain.network.monitor.tool.service

import altchain.network.monitor.tool.service.abfi.AbfiService
import altchain.network.monitor.tool.service.altchain.AltchainService
import altchain.network.monitor.tool.service.explorers.BtcExplorer
import altchain.network.monitor.tool.service.explorers.EthExplorer
import altchain.network.monitor.tool.service.miners.ApmMiner
import altchain.network.monitor.tool.service.miners.VpmMiner
import altchain.network.monitor.tool.service.nodecore.NodeCoreService
import org.koin.core.module.Module
import org.koin.dsl.module
import org.veriblock.core.utilities.Configuration

fun serviceModule(configuration: Configuration): Module {
    return module {
        single { BtcExplorer() }
        single { EthExplorer() }
        single {
            ExplorerService(
                setOf(
                    get<BtcExplorer>(),
                    get<EthExplorer>()
                )
            )
        }

        single { ApmMiner(get()) }
        single { VpmMiner(get()) }
        single {
            MinerService(
                setOf(
                    get<ApmMiner>(),
                    get<VpmMiner>()
                )
            )
        }

        single { NodeCoreService() }
        single { AbfiService() }
        single { MetricsService(get(), get()) }
        single { CleanupService(get(), get(), get(), get(), get(), get()) }

        single { AltchainService(get()) }

        single { MonitorService(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    }
}