package altchain.explorer.verifier.service

import altchain.explorer.verifier.service.explorers.BtcExplorer
import altchain.explorer.verifier.service.explorers.EthExplorer
import altchain.explorer.verifier.util.Configuration
import org.koin.core.module.Module
import org.koin.dsl.module

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

        single { ExplorerStatusService(get(), get(), get()) }
    }
}