package polycode.blockchain

import org.junit.jupiter.api.Test
import polycode.TestBase
import polycode.blockchain.properties.ChainPropertiesHandler
import polycode.blockchain.properties.ChainSpec
import polycode.config.ApplicationProperties
import polycode.config.ChainProperties
import polycode.exception.ErrorCode
import polycode.exception.UnsupportedChainIdException
import polycode.util.ChainId

class ChainPropertiesHandlerTest : TestBase() {

    companion object {
        private val CHAIN_ID = ChainId(1L)
        private val CHAINS = mapOf(
            CHAIN_ID to ChainProperties(
                name = "ETHEREUM_MAIN",
                rpcUrl = "rpc-url",
                infuraUrl = "/infura/",
                startBlockNumber = null,
                chainExplorerApiUrl = null,
                chainExplorerApiKey = null,
                minBlockConfirmationsForCaching = null
            )
        )
    }

    @Test
    fun mustCorrectlyCreateChainPropertiesWithServicesWhenRpcUrlIsNull() {
        val chainPropertiesHandler = suppose("chain properties handler is created from application properties") {
            ChainPropertiesHandler(
                ApplicationProperties().apply {
                    infuraId = ""
                    chain = CHAINS
                }
            )
        }

        verify("chain properties with services are correctly created") {
            val chainProperties = chainPropertiesHandler.getBlockchainProperties(CHAIN_ID.toSpec())
            expectThat(chainProperties.web3j).isNotNull()
        }
    }

    @Test
    fun mustCorrectlyCreateChainPropertiesWithServicesWhenCustomRpcUrlIsSpecified() {
        val chainPropertiesHandler = suppose("chain properties handler is created from application properties") {
            ChainPropertiesHandler(ApplicationProperties().apply { infuraId = "" })
        }

        verify("chain properties with services are correctly created") {
            val chainProperties = chainPropertiesHandler.getBlockchainProperties(
                ChainSpec(
                    chainId = ChainId(123L),
                    customRpcUrl = "http://localhost:1234/"
                )
            )
            expectThat(chainProperties.web3j).isNotNull()
        }
    }

    @Test
    fun mustCorrectlyCreateChainPropertiesWithServicesWhenCustomRpcUrlIsNotSpecified() {
        val chainPropertiesHandler = suppose("chain properties handler is created from application properties") {
            ChainPropertiesHandler(
                ApplicationProperties().apply {
                    infuraId = ""
                    chain = CHAINS
                }
            )
        }

        verify("chain properties with services are correctly created") {
            val chainProperties = chainPropertiesHandler.getBlockchainProperties(
                ChainSpec(
                    chainId = CHAIN_ID,
                    customRpcUrl = null
                )
            )
            expectThat(chainProperties.web3j).isNotNull()
        }
    }

    @Test
    fun mustThrowExceptionForInvalidChainId() {
        val chainPropertiesHandler = suppose("chain properties handler is created from application properties") {
            ChainPropertiesHandler(ApplicationProperties())
        }

        verify("InternalException is thrown") {
            val exception = expectThrows<UnsupportedChainIdException> {
                chainPropertiesHandler.getBlockchainProperties(ChainId(-1).toSpec())
            }
            expectThat(exception.errorCode).isEqualTo(ErrorCode.UNSUPPORTED_CHAIN_ID)
        }
    }

    @Test
    fun mustReturnDefaultRpcIfInfuraIdIsMissing() {
        val applicationProperties = ApplicationProperties()
            .apply {
                infuraId = ""
                chain = CHAINS
            }

        val chainPropertiesHandler = suppose("chain properties handler is created from application properties") {
            ChainPropertiesHandler(applicationProperties)
        }

        verify("correct RPC URL is returned") {
            val chainProperties = applicationProperties.chain[CHAIN_ID]!!
            val rpc = chainPropertiesHandler.getChainRpcUrl(chainProperties)
            expectThat(rpc).isEqualTo(chainProperties.rpcUrl)
        }
    }

    @Test
    fun mustReturnDefaultRpcWhenChainDoesNotHaveInfuraRpcDefined() {
        val applicationProperties = ApplicationProperties()
            .apply {
                infuraId = ""
                chain = CHAINS
            }

        val chainPropertiesHandler = suppose("chain properties handler is created from application properties") {
            ChainPropertiesHandler(applicationProperties)
        }

        verify("correct RPC URL is returned") {
            val chainProperties = applicationProperties.chain[CHAIN_ID]!!
            val rpc = chainPropertiesHandler.getChainRpcUrl(chainProperties)
            expectThat(rpc).isEqualTo(chainProperties.rpcUrl)
        }
    }

    @Test
    fun mustReturnInfuraRpc() {
        val infuraId = "some-id"
        val applicationProperties = ApplicationProperties()
            .apply {
                this.infuraId = infuraId
                chain = CHAINS
            }

        val chainPropertiesHandler = suppose("chain properties handler is created from application properties") {
            ChainPropertiesHandler(applicationProperties)
        }

        verify("correct Infura RPC URL is returned") {
            val chainProperties = applicationProperties.chain[CHAIN_ID]!!
            val rpc = chainPropertiesHandler.getChainRpcUrl(chainProperties)
            expectThat(rpc).isEqualTo(chainProperties.infuraUrl + infuraId)
        }
    }

    private fun ChainId.toSpec() = ChainSpec(this, null)
}
