## Altchain Explorer Verifier Tool
This tool it's used to check if a given alt chain explorer have ATVs, VTBs and VBKs on a given amount of blocks, 
the result its stored into a SQLite database, the results can be accessed through an API.

### Supported explorers:
* https://github.com/janoside/btc-rpc-explorer
* https://github.com/etherparty/explorer

### Configuration 
The configuration can be done through the 'application.conf' file:

| Configuration                 | Default                                       | Description                                                                                                                                      |
|-------------------------------|-----------------------------------------------|-------------------------------------------|
| api.port                      | 8080                                          | The API port                              |
| api.auth.username             |                                               | The API basic auth username               |
| api.auth.password             |                                               | The API basic auth password               |
| database.path                 | ./explorer-state.db                           | The database path and file name           |
| explorers.name.url            | https://testnet.explore.vbtc.veriblock.org    | The explorer url to check                 |
| explorers.name.blockCount     | 50                                            | The number of blocks to check             |
| explorers.name.type           | BTC                                           | The explorer type: BTC or ETH             |
| explorers.name.loadDelay      | 40                                            | The specific delay used by the parser     |
| explorers.name.checkDelay     | 10                                            | The delay in minutes between checks       |

Configuration example:
```
api {
    port = 8080
    auth {
        username = "root"
        password = "root"
    }
}

database {
    path = "./explorer-stats.db"
}

explorers {
    veriblockExplorer: {
        url = "https://testnet.explore.vbtc.veriblock.org"
        blockCount = 50
        type = "BTC"
        loadDelay = 40
        checkDelay = 10
    },
    bitcExplorer: {
        url = "https://testnet.explore.bitc.veriblock.org"
        blockCount = 50
        type = "BTC"
        loadDelay = 40
        checkDelay = 10
    }
}
```

### API Endpoints
The API documentation can be found at the swagger (http://localhost:8080/ by default)

### Build & run
* Run the command ```gradlew build``` to build the project (the build will appear at the ```build/distributions``` folder)
* Run ```altchain-explorer-verifier```