## Altchain Explorer Verifier Tool
This small tool it's used to check if a given alt chain explorer have ATVs, VTBs and VBKs on a given amount of blocks.

#### Supported explorers:
* https://github.com/janoside/btc-rpc-explorer
* https://github.com/etherparty/explorer

#### Usage:
The tool works with program arguments, those are the available ones:

|Name           |Default                                        | Description                           |
|---------------|-----------------------------------------------|---------------------------------------|
|blockCount     | 50                                            | The number of blocks to check         |
|explorerUrl    | https://testnet.explore.vbtc.veriblock.org    | The explorer url to check             |
|explorerType   | BTC                                           | The explorer type: BTC or ETH         |
|authUser       |                                               | The explorer auth user if any         |
|authPassword   |                                               | The explorer auth password if any     |
|loadDelay      | 40                                            | The specific delay used by the parser |

#### Build & run
* Run the command ```gradlew build``` to build the project (the build will appear at the ```build/distributions``` folder)
* Run ```altchain-explorer-verifier``` with the desired program arguments