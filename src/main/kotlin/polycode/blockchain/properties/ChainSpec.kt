package polycode.blockchain.properties

import polycode.util.ChainId

data class ChainSpec(
    val chainId: ChainId,
    val customRpcUrl: String?
)
