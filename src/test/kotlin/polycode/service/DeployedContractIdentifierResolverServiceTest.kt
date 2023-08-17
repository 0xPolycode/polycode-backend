package polycode.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import polycode.TestBase
import polycode.TestData
import polycode.exception.ContractNotYetDeployedException
import polycode.exception.ResourceNotFoundException
import polycode.features.api.access.model.result.Project
import polycode.features.contract.deployment.model.params.DeployedContractAddressIdentifier
import polycode.features.contract.deployment.model.params.DeployedContractAliasIdentifier
import polycode.features.contract.deployment.model.params.DeployedContractIdIdentifier
import polycode.features.contract.deployment.model.result.ContractDeploymentRequest
import polycode.features.contract.deployment.repository.ContractDeploymentRequestRepository
import polycode.features.contract.deployment.service.DeployedContractIdentifierResolverServiceImpl
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.model.DeserializableEvent
import polycode.model.ScreenConfig
import polycode.model.result.BlockchainTransactionInfo
import polycode.util.Balance
import polycode.util.BaseUrl
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.ContractBinaryData
import polycode.util.ContractId
import polycode.util.FunctionData
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import polycode.util.ZeroAddress
import java.math.BigInteger
import java.util.UUID

class DeployedContractIdentifierResolverServiceTest : TestBase() {

    companion object {
        private val PROJECT = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = UserId(UUID.randomUUID()),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = ChainId(1337L),
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        private val DEPLOYED_CONTRACT_ID = DeployedContractIdIdentifier(ContractDeploymentRequestId(UUID.randomUUID()))
        private val DEPLOYED_CONTRACT_ALIAS = DeployedContractAliasIdentifier("contract-alias")
        private val DEPLOYED_CONTRACT_ADDRESS = DeployedContractAddressIdentifier(ContractAddress("abc123"))
        private val DEPLOYER_ADDRESS = WalletAddress("a")
        private val TX_HASH = TransactionHash("deployed-contract-hash")
        private val DEPLOYED_CONTRACT = ContractDeploymentRequest(
            id = DEPLOYED_CONTRACT_ID.id,
            alias = DEPLOYED_CONTRACT_ALIAS.alias,
            name = "name",
            description = "description",
            contractId = ContractId("cid"),
            contractData = ContractBinaryData("00"),
            constructorParams = TestData.EMPTY_JSON_ARRAY,
            contractTags = emptyList(),
            contractImplements = emptyList(),
            initialEthAmount = Balance.ZERO,
            chainId = ChainId(1337L),
            redirectUrl = "redirect-url",
            projectId = PROJECT.id,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig.EMPTY,
            contractAddress = DEPLOYED_CONTRACT_ADDRESS.contractAddress,
            deployerAddress = DEPLOYER_ADDRESS,
            txHash = TX_HASH,
            imported = false,
            proxy = false,
            implementationContractAddress = null
        )
        private val BLOCKCHAIN_TRANSACTION_INFO = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = DEPLOYER_ADDRESS,
            to = ZeroAddress,
            deployedContractAddress = DEPLOYED_CONTRACT_ADDRESS.contractAddress,
            data = FunctionData(DEPLOYED_CONTRACT.contractData.value),
            value = DEPLOYED_CONTRACT.initialEthAmount,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )
        private val EVENTS = listOf<DeserializableEvent>()
    }

    @Test
    fun mustCorrectlyResolveContractIdAndAddressForDeployedContractId() {
        val repository = mock<ContractDeploymentRequestRepository>()

        suppose("deployed contract is returned from database") {
            call(repository.getById(DEPLOYED_CONTRACT_ID.id))
                .willReturn(DEPLOYED_CONTRACT)
        }

        val service = DeployedContractIdentifierResolverServiceImpl(repository, mock())

        verify("contract contract ID and address are resolved") {
            expectThat(service.resolveContractIdAndAddress(DEPLOYED_CONTRACT_ID, PROJECT))
                .isEqualTo(Pair(DEPLOYED_CONTRACT.id, DEPLOYED_CONTRACT.contractAddress))
        }
    }

    @Test
    fun mustCorrectlyResolveContractIdAndAddressAndSetContractAddressForDeployedContractId() {
        val repository = mock<ContractDeploymentRequestRepository>()

        suppose("deployed contract is returned from database") {
            call(repository.getById(DEPLOYED_CONTRACT_ID.id))
                .willReturn(DEPLOYED_CONTRACT.copy(contractAddress = null))
        }

        val ethCommonService = mock<EthCommonService>()

        suppose("deployed contract address will be fetched from blockchain") {
            call(
                ethCommonService.fetchTransactionInfo(
                    txHash = TX_HASH,
                    chainId = DEPLOYED_CONTRACT.chainId,
                    customRpcUrl = PROJECT.customRpcUrl,
                    events = EVENTS
                )
            ).willReturn(BLOCKCHAIN_TRANSACTION_INFO)
        }

        val service = DeployedContractIdentifierResolverServiceImpl(repository, ethCommonService)

        verify("contract contract ID and address are resolved") {
            expectThat(service.resolveContractIdAndAddress(DEPLOYED_CONTRACT_ID, PROJECT))
                .isEqualTo(Pair(DEPLOYED_CONTRACT.id, DEPLOYED_CONTRACT.contractAddress))
        }
    }

    @Test
    fun mustThrowContractNotYetDeployedExceptionWhenResolvingContractIdAndAddressForNonDeployedContractId() {
        val repository = mock<ContractDeploymentRequestRepository>()

        suppose("deployed contract is returned from database without contract address") {
            call(repository.getById(DEPLOYED_CONTRACT_ID.id))
                .willReturn(DEPLOYED_CONTRACT.copy(contractAddress = null))
        }

        val service = DeployedContractIdentifierResolverServiceImpl(repository, mock())

        verify("ContractNotYetDeployedException is thrown") {
            expectThrows<ContractNotYetDeployedException> {
                service.resolveContractIdAndAddress(DEPLOYED_CONTRACT_ID, PROJECT)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenResolvingContractIdAndAddressForNonExistentContractId() {
        val repository = mock<ContractDeploymentRequestRepository>()

        suppose("deployed contract is not found from database") {
            call(repository.getById(DEPLOYED_CONTRACT_ID.id))
                .willReturn(null)
        }

        val service = DeployedContractIdentifierResolverServiceImpl(repository, mock())

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.resolveContractIdAndAddress(DEPLOYED_CONTRACT_ID, PROJECT)
            }
        }
    }

    @Test
    fun mustCorrectlyResolveContractIdAndAddressForDeployedContractAlias() {
        val repository = mock<ContractDeploymentRequestRepository>()

        suppose("deployed contract is returned from database") {
            call(repository.getByAliasAndProjectId(DEPLOYED_CONTRACT_ALIAS.alias, PROJECT.id))
                .willReturn(DEPLOYED_CONTRACT)
        }

        val service = DeployedContractIdentifierResolverServiceImpl(repository, mock())

        verify("contract contract ID and address are resolved") {
            expectThat(service.resolveContractIdAndAddress(DEPLOYED_CONTRACT_ALIAS, PROJECT))
                .isEqualTo(Pair(DEPLOYED_CONTRACT.id, DEPLOYED_CONTRACT.contractAddress))
        }
    }

    @Test
    fun mustCorrectlyResolveContractIdAndAddressAndSetContractAddressForDeployedContractAlias() {
        val repository = mock<ContractDeploymentRequestRepository>()

        suppose("deployed contract is returned from database") {
            call(repository.getByAliasAndProjectId(DEPLOYED_CONTRACT_ALIAS.alias, PROJECT.id))
                .willReturn(DEPLOYED_CONTRACT.copy(contractAddress = null))
        }

        val ethCommonService = mock<EthCommonService>()

        suppose("deployed contract address will be fetched from blockchain") {
            call(
                ethCommonService.fetchTransactionInfo(
                    txHash = TX_HASH,
                    chainId = DEPLOYED_CONTRACT.chainId,
                    customRpcUrl = PROJECT.customRpcUrl,
                    events = EVENTS
                )
            ).willReturn(BLOCKCHAIN_TRANSACTION_INFO)
        }

        val service = DeployedContractIdentifierResolverServiceImpl(repository, ethCommonService)

        verify("contract contract ID and address are resolved") {
            expectThat(service.resolveContractIdAndAddress(DEPLOYED_CONTRACT_ALIAS, PROJECT))
                .isEqualTo(Pair(DEPLOYED_CONTRACT.id, DEPLOYED_CONTRACT.contractAddress))
        }
    }

    @Test
    fun mustThrowContractNotYetDeployedExceptionWhenResolvingContractIdAndAddressForNonDeployedContractAlias() {
        val repository = mock<ContractDeploymentRequestRepository>()

        suppose("deployed contract is returned from database without contract address") {
            call(repository.getByAliasAndProjectId(DEPLOYED_CONTRACT_ALIAS.alias, PROJECT.id))
                .willReturn(DEPLOYED_CONTRACT.copy(contractAddress = null))
        }

        val service = DeployedContractIdentifierResolverServiceImpl(repository, mock())

        verify("ContractNotYetDeployedException is thrown") {
            expectThrows<ContractNotYetDeployedException> {
                service.resolveContractIdAndAddress(DEPLOYED_CONTRACT_ALIAS, PROJECT)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenResolvingContractIdAndAddressForNonExistentContractAlias() {
        val repository = mock<ContractDeploymentRequestRepository>()

        suppose("deployed contract is not found from database") {
            call(repository.getByAliasAndProjectId(DEPLOYED_CONTRACT_ALIAS.alias, PROJECT.id))
                .willReturn(null)
        }

        val service = DeployedContractIdentifierResolverServiceImpl(repository, mock())

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.resolveContractIdAndAddress(DEPLOYED_CONTRACT_ALIAS, PROJECT)
            }
        }
    }

    @Test
    fun mustCorrectlyResolveContractIdAndAddressForDeployedContractAddress() {
        val service = DeployedContractIdentifierResolverServiceImpl(mock(), mock())

        verify("contract contract ID and address are resolved") {
            expectThat(service.resolveContractIdAndAddress(DEPLOYED_CONTRACT_ADDRESS, PROJECT))
                .isEqualTo(Pair(null, DEPLOYED_CONTRACT.contractAddress))
        }
    }
}
