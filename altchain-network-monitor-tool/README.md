## Altchain Network Monitor Tool
The Altchain Network Monitor Tool is a tool capable to monitor one or multiple VeriBlock Altchains and all the pieces involved in the network in a single place, 
providing valuable information from the network status and all the involved parts.

All the generated information is stored into a SQLite database so that the results can be accessed through an API.

### Supported Software
The tool is capable to monitor any amount of instances of the next pieces of software:
* VeriBlock NodeCore daemon
* VeriBlock Altchain Block Finality Indicator (vBFI)
* [VeriBlock Altchain PoP Miner (APM)](https://github.com/VeriBlock/nodecore/tree/master/pop-miners/altchain-pop-miner)
* [VeriBlock PoP Miner (VPM)](https://github.com/VeriBlock/nodecore/tree/master/pop-miners/veriblock-pop-miner)
* [Any Altchain daemon supported by the VeriBlock Altchain Plugins](https://github.com/VeriBlock/nodecore/tree/master/altchain-plugins)
* [Bitcoin](https://github.com/janoside/btc-rpc-explorer) and [ETH](https://github.com/etherparty/explorer) block explorers

### Configuration

#### Initial configuration
The initial configuration can be done through the 'application.conf' file:

| Configuration                 | Default                                       | Description                                                                                                                                      |
|-------------------------------|-----------------------------------------------|-------------------------------------------|
| api.port                      | 8080                                          | The API port                              |
| api.auth.username             |                                               | The API basic auth username               |
| api.auth.password             |                                               | The API basic auth password               |
| database.path                 | ./network-monitor-tool.db                     | The database path and file name           |

Configuration example:
```
api {
    port = 8080
    # Uncomment the auth block if you want your API to be protected with basic auth
    #auth {
    #    username = "root"
    #    password = "root"
    #}
}

database {
    path = "./network-monitor-tool.db"
}
```

#### Altchain configuration
The Altchain configuration should go inside the network-configs folder, we will use the vBTC.conf as an example:

| Configuration                             | Default                                       | Description                                                                                                                                      |
|-------------------------------------------|-----------------------------------------------|-------------------------------------------|
| vBTC.checkDelay                           | 10                                            | The delay in minutes between the checks   |
| vBTC.minPercentageHealthyNodeCores        | 90                                            | The minimum amount of healthy NodeCores at the network so it can be considered as healthy |
| vBTC.minPercentageHealthyAltDaemons       | 90                                            | The minimum amount of healthy Altchain daemons at the network so it can be considered as healthy |
| vBTC.minPercentageHealthyExplorers        | 100                                           | The minimum amount of healthy Explorers at the network so it can be considered as healthy |
| vBTC.minPercentageHealthyAbfis            | 100                                           | The minimum amount of healthy Altchain Bitcoin Finality Indicator (ABFI) at the network so it can be considered as healthy |
| vBTC.minPercentageHealthyVpms             | 90                                            | The minimum amount of healthy PoP Miners (VPM) at the network so it can be considered as healthy |
| vBTC.maxPercentageNotHealthyVpmOperations | 15                                            | The maximum amount of failed operations from the VPM so it can be considered as not healthy |
| vBTC.minPercentageHealthyApms             | 80                                            | The minimum amount of healthy Altchain Miners (APM) at the network so it can be considered as healthy |
| vBTC.maxPercentageNotHealthyApmOperations | 15                                            | The maximum amount of failed operations from the APM so it can be considered as not healthy|
| vBTC.maxHealthyByTime                     | 10                                            | The maximum age in minutes to consider the collected data as valid |
| vBTC.nodecores.nodecore1.host             | localhost                                     | The NodeCore host |
| vBTC.nodecores.nodecore1.port             | 10500                                         | The NodeCore port |
| vBTC.nodecores.nodecore1.ssl              | false                                         | Specify if the connection with the NodeCore should use the SSL protocol |                                
| vBTC.nodecores.nodecore1.password         | mypassword                                    | The NodeCore RPC password (if any) |
| vBTC.altDaemons.altDaemon1.siKey          | vbtc1                                         | The securityInheriting configuration related with this node |
| vBTC.abfis.abfi1.apiUrl                   | https://testnet.abfi.veriblock.org            | The ABFI API url |
| vBTC.abfis.abfi1.prefix                   | 3860170                                       | The altchain prefix |
| vBTC.explorers.explorer1.url              | https://testnet.explore.vbtc.veriblock.org    | The explorer url |
| vBTC.explorers.explorer1.blockCount       | 50                                            | The amount of blocks to be checked |
| vBTC.explorers.explorer1.type             | BTC                                           | The explorer type, there are two options: BTC or ETH |
| vBTC.explorers.explorer1.loadDelay        | 40                                            | The delay in seconds between the explorer page-load and the block check |
| vBTC.explorers.explorer1.auth.username    | myusername                                    | The explorer auth username (if any) |
| vBTC.explorers.explorer1.auth.password    | mypassword                                    | The explorer auth password (if any) |
| vBTC.miners.vpm1.apiUrl                   | http://localhost:8080                         | The miner API url |
| vBTC.miners.vpm1.type                     | VPM                                           | The miner type, there are two options: APM or VPM |
| vBTC.miners.vpm1.auth.password            | myusername                                    | The miner auth username (if any) |
| vBTC.miners.vpm1.auth.password            | mypassword                                    | The miner auth password (if any) |
| vBTC.miners.apm1.apiUrl                   | http://localhost:8080                         | The miner API url |
| vBTC.miners.apm1.type                     | APM                                           | The miner type, there are two options: APM or VPM |
| vBTC.miners.apm1.auth.password            | myusername                                    | The miner auth username (if any) |
| vBTC.miners.apm1.auth.password            | mypassword                                    | The miner auth password (if any) |

``
NOTE: Please remember that the file name should match the configuration block, i.e: if the file name is vBTC.conf, the main configuration block should be vBTC
``

Configuration example:
```
vBTC: {
    checkDelay = 10 # Minutes
    minPercentageHealthyNodeCores = 90
    minPercentageHealthyAltDaemons = 90
    minPercentageHealthyExplorers = 100
    minPercentageHealthyAbfis = 100
    minPercentageHealthyVpms = 90
    maxPercentageNotHealthyVpmOperations = 15
    minPercentageHealthyApms = 80
    maxPercentageNotHealthyApmOperations = 15
    maxHealthyByTime = 10 # Minutes, recommended same as checkDelay or slightly higher

    nodecores: {
        nodecore1: {
            host = "localhost"
            port = 10500
            ssl = false
            # Uncomment the password if your NodeCore RPC is protected with password
            # password = "mypassword"
        }
    }
    altDaemons: {
        altDaemon1: {
            siKey = "vbtc1"
        }
    }
    abfis: {
        abfi1: {
            apiUrl = "https://testnet.abfi.veriblock.org"
            prefix = "3860170"
        }
    }
    explorers: {
        explorer1: {
            url = "https://testnet.explore.vbtc.veriblock.org"
            blockCount = 50
            type = "BTC"
            loadDelay = 40
            # Uncomment the auth block if your Explorer is protected with basic auth
            #auth {
            #    username = "myusername"
            #    password = "mypassword"
            #}
        }
    }
    miners: {
        vpm1: {
            apiUrl = "http://localhost:8080"
            type = "VPM"
            # Uncomment the auth block if your VPM miner API is protected with basic auth
            #auth {
            #    username = "myusername"
            #    password = "mypassword"
            #}
        }
        apm1: {
            apiUrl = "http://localhost:8080"
            type = "APM"
            # Uncomment the auth block if your APM miner API is protected with basic auth
            #auth {
            #    username = "myusername"
            #    password = "mypassword"
            #}
        }
    }
}

securityInheriting {
    vbtc1: {
        pluginKey: btc
        id: 3860170
        name: "vBitcoin"
        host: "http://localhost:18332"
        # Uncomment the auth block if your Alt Daemon is protected with basic auth
        #auth: {
        #    username: "myusername"
        #    password: "mypassword"
        #}
    }
}
```

### Logic behind each health check
The Altchain Network Monitor Tool collects big data samples from the different network components to decide if the network is healthy or not. 
An Altchain Network is considered healthy only if all its involved components are healthy as well. 

Here you can see what data is used to decide the healthy status of each kind of component: 

* Global (for all the components):
  * The tool collects data samples every ``checkDelay``, it uses the newest of these samples only if its age is less than ``maxHealthyByTime``
* NodeCore:
  * The configured daemons are synchronized
  * The amount of healthy daemons is greater or equal than the configured ``minPercentageHealthyNodeCores``
* Altchain daemon:
  * The configured daemons are synchronized
  * The amount of healthy daemons is greater or equal than the configured ``minPercentageHealthyAltDaemons``
* Altchain Bitcoin Finality Indicator:
  * The ``lastFinalizedBlockBtc`` property is present
  * The amount of healthy ABFIs is greater or equal than the configured ``minPercentageHealthyAbfis``
* Explorer:
  * There is at least one ATV, VTB and VBK on the last configured ``blockCount``
  * The amount of healthy explorers is greater or equal than the configured ``minPercentageHealthyExplorers``
* Miners:
  * VPM:
    * The percentage of failed operations is lower than the configured ``maxPercentageNotHealthyVpmOperations``
    * The amount of healthy miners is greater or equal than the configured ``minPercentageHealthyVpms``
  * APM:
    * The percentage of failed operations is lower than the configured ``maxPercentageNotHealthyApmOperations``
    * The amount of healthy miners is greater or equal than the configured ``minPercentageHealthyApms``


### API Endpoints
The API documentation can be found at the swagger (http://localhost:8080/ by default)

### Build & run
* Run the command ```gradlew build``` to build the project (the build will appear at the ```build/distributions``` folder)
* Run ```altchain-network-monitor-tool```