package polycode.blockchain.properties

import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import polycode.config.ApplicationProperties
import polycode.config.ChainProperties
import polycode.exception.UnsupportedChainIdException
import polycode.util.ChainId
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

class ChainPropertiesHandler(private val applicationProperties: ApplicationProperties) {

    private val blockchainPropertiesMap = ConcurrentHashMap<ChainId, ChainPropertiesWithServices>()

    fun getBlockchainProperties(chainSpec: ChainSpec): ChainPropertiesWithServices {
        val chainProperties = applicationProperties.chain[chainSpec.chainId]

        return if (chainSpec.customRpcUrl != null) {
            ChainPropertiesWithServices(
                web3j = Web3j.build(HttpService(chainSpec.customRpcUrl)),
                latestBlockCacheDuration = chainProperties?.latestBlockCacheDuration ?: Duration.ZERO,
                minBlockConfirmationsForCaching = chainProperties?.minBlockConfirmationsForCaching
            )
        } else if (chainProperties != null) {
            blockchainPropertiesMap.computeIfAbsent(chainSpec.chainId) {
                generateBlockchainProperties(chainProperties)
            }
        } else {
            throw UnsupportedChainIdException(chainSpec.chainId)
        }
    }

    internal fun getChainRpcUrl(chainProperties: ChainProperties): String =
        if (chainProperties.infuraUrl == null || applicationProperties.infuraId.isBlank()) {
            chainProperties.rpcUrl
        } else {
            "${chainProperties.infuraUrl}${applicationProperties.infuraId}"
        }

    private fun generateBlockchainProperties(chainProperties: ChainProperties): ChainPropertiesWithServices {
        val rpcUrl = getChainRpcUrl(chainProperties)
        return ChainPropertiesWithServices(
            web3j = Web3j.build(HttpService(rpcUrl)),
            latestBlockCacheDuration = chainProperties.latestBlockCacheDuration,
            minBlockConfirmationsForCaching = chainProperties.minBlockConfirmationsForCaching
        )
    }
}
