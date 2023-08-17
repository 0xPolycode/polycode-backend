package polycode.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.web3j.abi.datatypes.generated.Uint256
import polycode.TestBase
import polycode.TestData
import polycode.blockchain.BlockchainService
import polycode.blockchain.properties.ChainSpec
import polycode.features.api.access.model.result.Project
import polycode.features.contract.abi.model.UintType
import polycode.features.contract.deployment.model.params.DeployedContractIdIdentifier
import polycode.features.contract.deployment.model.result.ContractDeploymentRequest
import polycode.features.contract.deployment.repository.ContractDeploymentRequestRepository
import polycode.features.contract.deployment.service.DeployedContractIdentifierResolverServiceImpl
import polycode.features.contract.readcall.model.params.CreateReadonlyFunctionCallParams
import polycode.features.contract.readcall.model.params.ExecuteReadonlyFunctionCallParams
import polycode.features.contract.readcall.model.params.OutputParameter
import polycode.features.contract.readcall.model.result.ReadonlyFunctionCallResult
import polycode.features.contract.readcall.service.ContractReadonlyFunctionCallServiceImpl
import polycode.features.functions.encoding.model.FunctionArgument
import polycode.features.functions.encoding.service.FunctionEncoderService
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.model.ScreenConfig
import polycode.util.Balance
import polycode.util.BaseUrl
import polycode.util.BlockName
import polycode.util.BlockNumber
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.ContractBinaryData
import polycode.util.ContractId
import polycode.util.FunctionData
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import polycode.util.WithDeployedContractIdAndAddress
import java.math.BigInteger
import java.util.UUID

class ContractReadonlyFunctionCallServiceTest : TestBase() {

    companion object {
        private val PROJECT = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = UserId(UUID.randomUUID()),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = ChainId(1337L),
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        private val DEPLOYED_CONTRACT_ID = ContractDeploymentRequestId(UUID.randomUUID())
        private val CONTRACT_ADDRESS = ContractAddress("abc123")
        private val CALLER_ADDRESS = WalletAddress("a")
        private val CREATE_PARAMS = CreateReadonlyFunctionCallParams(
            identifier = DeployedContractIdIdentifier(DEPLOYED_CONTRACT_ID),
            blockNumber = null,
            functionName = "example",
            functionParams = listOf(FunctionArgument(Uint256(BigInteger.TEN))),
            outputParams = listOf(OutputParameter(UintType)),
            callerAddress = CALLER_ADDRESS
        )
        private val ENCODED_FUNCTION_DATA = FunctionData("0x1234")
        private val CHAIN_SPEC = ChainSpec(PROJECT.chainId, PROJECT.customRpcUrl)
        private val DEPLOYED_CONTRACT = ContractDeploymentRequest(
            id = DEPLOYED_CONTRACT_ID,
            alias = "contract-alias",
            name = "name",
            description = "description",
            contractId = ContractId("cid"),
            contractData = ContractBinaryData("00"),
            constructorParams = TestData.EMPTY_JSON_ARRAY,
            contractTags = emptyList(),
            contractImplements = emptyList(),
            initialEthAmount = Balance.ZERO,
            chainId = CHAIN_SPEC.chainId,
            redirectUrl = "redirect-url",
            projectId = PROJECT.id,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig.EMPTY,
            contractAddress = CONTRACT_ADDRESS,
            deployerAddress = CALLER_ADDRESS,
            txHash = TransactionHash("deployed-contract-hash"),
            imported = false,
            proxy = false,
            implementationContractAddress = null
        )
    }

    @Test
    fun mustCorrectlyCallReadonlyFunction() {
        val functionEncoderService = mock<FunctionEncoderService>()
        val createParams = CREATE_PARAMS

        suppose("function will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = createParams.functionName,
                    arguments = createParams.functionParams
                )
            )
                .willReturn(ENCODED_FUNCTION_DATA)
        }

        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("deployed contract is returned from database") {
            call(contractDeploymentRequestRepository.getById(DEPLOYED_CONTRACT_ID))
                .willReturn(DEPLOYED_CONTRACT)
        }

        val blockchainService = mock<BlockchainService>()
        val readonlyFunctionCallResult = ReadonlyFunctionCallResult(
            blockNumber = BlockNumber(BigInteger.ONE),
            timestamp = TestData.TIMESTAMP,
            returnValues = listOf(BigInteger.TWO),
            rawReturnValue = "0x0"
        )

        suppose("blockchain service will return some value for readonly function call") {
            call(
                blockchainService.callReadonlyFunction(
                    chainSpec = CHAIN_SPEC,
                    params = ExecuteReadonlyFunctionCallParams(
                        contractAddress = CONTRACT_ADDRESS,
                        callerAddress = CALLER_ADDRESS,
                        functionName = createParams.functionName,
                        functionData = ENCODED_FUNCTION_DATA,
                        outputParams = listOf(OutputParameter(UintType))
                    ),
                    blockParameter = BlockName.LATEST
                )
            ).willReturn(readonlyFunctionCallResult)
        }

        val service = ContractReadonlyFunctionCallServiceImpl(
            functionEncoderService = functionEncoderService,
            deployedContractIdentifierResolverService = service(contractDeploymentRequestRepository),
            blockchainService = blockchainService
        )

        verify("contract readonly function call is correctly executed") {
            expectThat(
                service.callReadonlyContractFunction(createParams, PROJECT)
            ).isEqualTo(
                WithDeployedContractIdAndAddress(
                    value = readonlyFunctionCallResult,
                    deployedContractId = DEPLOYED_CONTRACT_ID,
                    contractAddress = CONTRACT_ADDRESS
                )
            )
        }
    }

    private fun service(repository: ContractDeploymentRequestRepository) =
        DeployedContractIdentifierResolverServiceImpl(repository, mock())
}
