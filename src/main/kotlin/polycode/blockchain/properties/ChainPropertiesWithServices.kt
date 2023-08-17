package polycode.blockchain.properties

import org.web3j.protocol.Web3j
import java.math.BigInteger
import java.time.Duration

data class ChainPropertiesWithServices(
    val web3j: Web3j,
    val latestBlockCacheDuration: Duration,
    val minBlockConfirmationsForCaching: BigInteger?
) {
    fun shouldCache(blockConfirmations: BigInteger): Boolean =
        minBlockConfirmationsForCaching != null && blockConfirmations >= minBlockConfirmationsForCaching
}
