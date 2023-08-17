package polycode.features.contract.arbitrarycall.service

import org.jooq.JSON
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import polycode.TestBase
import polycode.TestData
import polycode.blockchain.BlockchainService
import polycode.blockchain.properties.ChainSpec
import polycode.config.JsonConfig
import polycode.exception.CannotAttachTxInfoException
import polycode.exception.ResourceNotFoundException
import polycode.features.api.access.model.result.Project
import polycode.features.api.access.repository.ProjectRepository
import polycode.features.blacklist.service.BlacklistCheckService
import polycode.features.contract.arbitrarycall.model.filters.ContractArbitraryCallRequestFilters
import polycode.features.contract.arbitrarycall.model.params.CreateContractArbitraryCallRequestParams
import polycode.features.contract.arbitrarycall.model.params.StoreContractArbitraryCallRequestParams
import polycode.features.contract.arbitrarycall.model.result.ContractArbitraryCallRequest
import polycode.features.contract.arbitrarycall.repository.ContractArbitraryCallRequestRepository
import polycode.features.contract.deployment.model.params.DeployedContractIdIdentifier
import polycode.features.contract.deployment.model.result.ContractDeploymentRequest
import polycode.features.contract.deployment.repository.ContractDeploymentRequestRepository
import polycode.features.contract.deployment.service.DeployedContractIdentifierResolverServiceImpl
import polycode.features.functions.decoding.model.EthFunction
import polycode.features.functions.decoding.service.FunctionDecoderService
import polycode.features.functions.encoding.model.FunctionArgument
import polycode.generated.jooq.id.ContractArbitraryCallRequestId
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.model.DeserializableEvent
import polycode.model.ScreenConfig
import polycode.model.result.BlockchainTransactionInfo
import polycode.service.EthCommonServiceImpl
import polycode.service.UtcDateTimeProvider
import polycode.service.UuidProvider
import polycode.util.Balance
import polycode.util.BaseUrl
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.ContractBinaryData
import polycode.util.ContractId
import polycode.util.FunctionData
import polycode.util.JsonNodeConverter
import polycode.util.Status
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import java.math.BigInteger
import java.util.UUID

class ContractArbitraryCallRequestServiceTest : TestBase() {

    companion object {
        private val PROJECT = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = UserId(UUID.randomUUID()),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = ChainId(1337L),
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        private val OBJECT_MAPPER = JsonConfig().objectMapper()
        private val DEPLOYED_CONTRACT_ID = ContractDeploymentRequestId(UUID.randomUUID())
        private val ENCODED_FUNCTION_DATA = FunctionData("0x1234")
        private val DEPLOYED_CONTRACT_ID_CREATE_PARAMS = CreateContractArbitraryCallRequestParams(
            identifier = DeployedContractIdIdentifier(DEPLOYED_CONTRACT_ID),
            functionData = ENCODED_FUNCTION_DATA,
            ethAmount = Balance(BigInteger("10000")),
            redirectUrl = "redirect-url/\${id}",
            callerAddress = WalletAddress("a"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        private const val FUNCTION_NAME = "test"
        private val RAW_FUNCTION_PARAMS = JsonNodeConverter().from(
            JSON.valueOf("[{\"type\": \"string\", \"value\": \"test\"}]")
        )!!
        private val FUNCTION_PARAMS = OBJECT_MAPPER.treeToValue(
            RAW_FUNCTION_PARAMS, Array<FunctionArgument>::class.java
        ).toList()
        private val CONTRACT_ADDRESS = ContractAddress("abc123")
        private val ID = ContractArbitraryCallRequestId(UUID.randomUUID())
        private val STORE_PARAMS = StoreContractArbitraryCallRequestParams(
            id = ID,
            deployedContractId = DEPLOYED_CONTRACT_ID,
            contractAddress = CONTRACT_ADDRESS,
            functionData = ENCODED_FUNCTION_DATA,
            functionName = FUNCTION_NAME,
            functionParams = RAW_FUNCTION_PARAMS,
            ethAmount = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.ethAmount,
            chainId = PROJECT.chainId,
            redirectUrl = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.redirectUrl!!.replace("\${id}", ID.value.toString()),
            projectId = PROJECT.id,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.arbitraryData,
            screenConfig = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.screenConfig,
            callerAddress = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.callerAddress
        )
        private val TX_HASH = TransactionHash("tx-hash")
        private val STORED_REQUEST = ContractArbitraryCallRequest(
            id = ID,
            deployedContractId = DEPLOYED_CONTRACT_ID,
            contractAddress = CONTRACT_ADDRESS,
            functionData = STORE_PARAMS.functionData,
            functionName = STORE_PARAMS.functionName,
            functionParams = STORE_PARAMS.functionParams,
            ethAmount = STORE_PARAMS.ethAmount,
            chainId = STORE_PARAMS.chainId,
            redirectUrl = STORE_PARAMS.redirectUrl,
            projectId = PROJECT.id,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = STORE_PARAMS.arbitraryData,
            screenConfig = STORE_PARAMS.screenConfig,
            callerAddress = STORE_PARAMS.callerAddress,
            txHash = TX_HASH
        )
        private val CHAIN_SPEC = ChainSpec(STORED_REQUEST.chainId, null)
        private val TRANSACTION_INFO = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = STORED_REQUEST.callerAddress!!,
            to = CONTRACT_ADDRESS,
            deployedContractAddress = null,
            data = ENCODED_FUNCTION_DATA,
            value = STORED_REQUEST.ethAmount,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )
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
            screenConfig = STORE_PARAMS.screenConfig,
            contractAddress = CONTRACT_ADDRESS,
            deployerAddress = STORE_PARAMS.callerAddress,
            txHash = TransactionHash("deployed-contract-hash"),
            imported = false,
            proxy = false,
            implementationContractAddress = null
        )
        private val EVENTS = listOf<DeserializableEvent>()
    }

    @Test
    fun mustCorrectlyCreateArbitraryCallRequestWhenContractAddressIsNotBlacklisted() {
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be generated") {
            call(uuidProvider.getRawUuid())
                .willReturn(ID.value)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val functionDecoderService = mock<FunctionDecoderService>()
        val createParams = DEPLOYED_CONTRACT_ID_CREATE_PARAMS

        suppose("function will be decoded") {
            call(functionDecoderService.decode(ENCODED_FUNCTION_DATA))
                .willReturn(EthFunction(FUNCTION_NAME, FUNCTION_PARAMS))
        }

        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("deployed contract is returned from database") {
            call(contractDeploymentRequestRepository.getById(DEPLOYED_CONTRACT_ID))
                .willReturn(DEPLOYED_CONTRACT)
        }

        val contractArbitraryCallRequestRepository = mock<ContractArbitraryCallRequestRepository>()

        suppose("contract arbitrary call request is stored in database") {
            call(contractArbitraryCallRequestRepository.store(STORE_PARAMS))
                .willReturn(STORED_REQUEST)
        }

        val blacklistCheckService = mock<BlacklistCheckService>()

        suppose("contract address is not blacklisted") {
            call(blacklistCheckService.exists(CONTRACT_ADDRESS))
                .willReturn(false)
        }

        val service = ContractArbitraryCallRequestServiceImpl(
            functionDecoderService = functionDecoderService,
            contractArbitraryCallRequestRepository = contractArbitraryCallRequestRepository,
            deployedContractIdentifierResolverService = service(contractDeploymentRequestRepository),
            contractDeploymentRequestRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = mock(),
            blacklistCheckService = blacklistCheckService,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = uuidProvider,
                utcDateTimeProvider = utcDateTimeProvider,
                blockchainService = mock()
            ),
            projectRepository = mock(),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("contract arbitrary call request is correctly created") {
            expectThat(service.createContractArbitraryCallRequest(createParams, PROJECT))
                .isEqualTo(STORED_REQUEST)

            expectInteractions(contractArbitraryCallRequestRepository) {
                once.store(STORE_PARAMS)
            }
        }
    }

    @Test
    fun mustCorrectlyCreateArbitraryCallRequestWhenContractAddressIsBlacklisted() {
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be generated") {
            call(uuidProvider.getRawUuid())
                .willReturn(ID.value)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val functionDecoderService = mock<FunctionDecoderService>()
        val createParams = DEPLOYED_CONTRACT_ID_CREATE_PARAMS

        suppose("function will be decoded") {
            call(
                functionDecoderService.decode(ENCODED_FUNCTION_DATA)
            )
                .willReturn(EthFunction(FUNCTION_NAME, FUNCTION_PARAMS))
        }

        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("deployed contract is returned from database") {
            call(contractDeploymentRequestRepository.getById(DEPLOYED_CONTRACT_ID))
                .willReturn(DEPLOYED_CONTRACT)
        }

        val storeParams = STORE_PARAMS.copy(redirectUrl = STORE_PARAMS.redirectUrl + "/caution")
        val storedRequest = STORED_REQUEST.copy(redirectUrl = STORED_REQUEST.redirectUrl + "/caution")
        val contractArbitraryCallRequestRepository = mock<ContractArbitraryCallRequestRepository>()

        suppose("contract arbitrary call request is stored in database") {
            call(contractArbitraryCallRequestRepository.store(storeParams))
                .willReturn(storedRequest)
        }

        val blacklistCheckService = mock<BlacklistCheckService>()

        suppose("contract address is not blacklisted") {
            call(blacklistCheckService.exists(CONTRACT_ADDRESS))
                .willReturn(true)
        }

        val service = ContractArbitraryCallRequestServiceImpl(
            functionDecoderService = functionDecoderService,
            contractArbitraryCallRequestRepository = contractArbitraryCallRequestRepository,
            deployedContractIdentifierResolverService = service(contractDeploymentRequestRepository),
            contractDeploymentRequestRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = mock(),
            blacklistCheckService = blacklistCheckService,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = uuidProvider,
                utcDateTimeProvider = utcDateTimeProvider,
                blockchainService = mock()
            ),
            projectRepository = mock(),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("contract arbitrary call request is correctly created") {
            expectThat(service.createContractArbitraryCallRequest(createParams, PROJECT))
                .isEqualTo(storedRequest)

            expectInteractions(contractArbitraryCallRequestRepository) {
                once.store(storeParams)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionForNonExistentContractArbitraryCallRequest() {
        val contractArbitraryCallRequestRepository = mock<ContractArbitraryCallRequestRepository>()

        suppose("contract arbitrary call request is not found in the database") {
            call(contractArbitraryCallRequestRepository.getById(ID))
                .willReturn(null)
        }

        val service = ContractArbitraryCallRequestServiceImpl(
            functionDecoderService = mock(),
            contractArbitraryCallRequestRepository = contractArbitraryCallRequestRepository,
            deployedContractIdentifierResolverService = service(mock()),
            contractDeploymentRequestRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = mock(),
            blacklistCheckService = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock(),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.getContractArbitraryCallRequest(ID)
            }
        }
    }

    @Test
    fun mustReturnContractArbitraryCallRequestWithPendingStatusWhenContractArbitraryCallRequestHasNullTxHash() {
        val contractArbitraryCallRequestRepository = mock<ContractArbitraryCallRequestRepository>()
        val request = STORED_REQUEST.copy(txHash = null)

        suppose("contract arbitrary call request exists in the database") {
            call(contractArbitraryCallRequestRepository.getById(ID))
                .willReturn(request)
        }

        val service = ContractArbitraryCallRequestServiceImpl(
            functionDecoderService = mock(),
            contractArbitraryCallRequestRepository = contractArbitraryCallRequestRepository,
            deployedContractIdentifierResolverService = service(mock()),
            contractDeploymentRequestRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = mock(),
            blacklistCheckService = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(PROJECT.id),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("contract arbitrary call request with pending status is returned") {
            expectThat(service.getContractArbitraryCallRequest(ID))
                .isEqualTo(
                    request.withTransactionData(
                        status = Status.PENDING,
                        transactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnContractArbitraryCallRequestWithPendingStatusWhenTransactionIsNotYetMined() {
        val contractArbitraryCallRequestRepository = mock<ContractArbitraryCallRequestRepository>()
        val request = STORED_REQUEST

        suppose("contract arbitrary call request exists in the database") {
            call(contractArbitraryCallRequestRepository.getById(ID))
                .willReturn(request)
        }

        val blockchainService = mock<BlockchainService>()

        suppose("transaction is not yet mined") {
            call(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH, EVENTS))
                .willReturn(null)
        }

        val service = ContractArbitraryCallRequestServiceImpl(
            functionDecoderService = mock(),
            contractArbitraryCallRequestRepository = contractArbitraryCallRequestRepository,
            deployedContractIdentifierResolverService = service(mock()),
            contractDeploymentRequestRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = mock(),
            blacklistCheckService = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("contract arbitrary call request with pending status is returned") {
            expectThat(service.getContractArbitraryCallRequest(ID))
                .isEqualTo(
                    request.withTransactionData(
                        status = Status.PENDING,
                        transactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnContractArbitraryCallRequestWithFailedStatusWhenTransactionIsNotSuccessful() {
        val contractArbitraryCallRequestRepository = mock<ContractArbitraryCallRequestRepository>()
        val request = STORED_REQUEST

        suppose("contract arbitrary call request exists in the database") {
            call(contractArbitraryCallRequestRepository.getById(ID))
                .willReturn(request)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(success = false)

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val service = ContractArbitraryCallRequestServiceImpl(
            functionDecoderService = mock(),
            contractArbitraryCallRequestRepository = contractArbitraryCallRequestRepository,
            deployedContractIdentifierResolverService = service(mock()),
            contractDeploymentRequestRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = mock(),
            blacklistCheckService = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("contract arbitrary call request with failed status is returned") {
            expectThat(service.getContractArbitraryCallRequest(ID))
                .isEqualTo(
                    request.withTransactionData(
                        status = Status.FAILED,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractArbitraryCallRequestWithFailedStatusWhenTransactionHasWrongTxHash() {
        val contractArbitraryCallRequestRepository = mock<ContractArbitraryCallRequestRepository>()
        val request = STORED_REQUEST

        suppose("contract arbitrary call request exists in the database") {
            call(contractArbitraryCallRequestRepository.getById(ID))
                .willReturn(request)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(hash = TransactionHash("other-tx-hash"))

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val service = ContractArbitraryCallRequestServiceImpl(
            functionDecoderService = mock(),
            contractArbitraryCallRequestRepository = contractArbitraryCallRequestRepository,
            deployedContractIdentifierResolverService = service(mock()),
            contractDeploymentRequestRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = mock(),
            blacklistCheckService = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("contract arbitrary call request with failed status is returned") {
            expectThat(service.getContractArbitraryCallRequest(ID))
                .isEqualTo(
                    request.withTransactionData(
                        status = Status.FAILED,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractArbitraryCallRequestWithFailedStatusWhenTransactionHasWrongCallerAddress() {
        val contractArbitraryCallRequestRepository = mock<ContractArbitraryCallRequestRepository>()
        val request = STORED_REQUEST

        suppose("contract arbitrary call request exists in the database") {
            call(contractArbitraryCallRequestRepository.getById(ID))
                .willReturn(request)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(from = WalletAddress("dead"))

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val service = ContractArbitraryCallRequestServiceImpl(
            functionDecoderService = mock(),
            contractArbitraryCallRequestRepository = contractArbitraryCallRequestRepository,
            deployedContractIdentifierResolverService = service(mock()),
            contractDeploymentRequestRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = mock(),
            blacklistCheckService = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("contract arbitrary call request with failed status is returned") {
            expectThat(service.getContractArbitraryCallRequest(ID))
                .isEqualTo(
                    request.withTransactionData(
                        status = Status.FAILED,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractArbitraryCallRequestWithFailedStatusWhenTransactionHasWrongContractAddress() {
        val contractArbitraryCallRequestRepository = mock<ContractArbitraryCallRequestRepository>()
        val request = STORED_REQUEST

        suppose("contract arbitrary call request exists in the database") {
            call(contractArbitraryCallRequestRepository.getById(ID))
                .willReturn(request)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(to = ContractAddress("dead"))

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val service = ContractArbitraryCallRequestServiceImpl(
            functionDecoderService = mock(),
            contractArbitraryCallRequestRepository = contractArbitraryCallRequestRepository,
            deployedContractIdentifierResolverService = service(mock()),
            contractDeploymentRequestRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = mock(),
            blacklistCheckService = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("contract arbitrary call request with failed status is returned") {
            expectThat(service.getContractArbitraryCallRequest(ID))
                .isEqualTo(
                    request.withTransactionData(
                        status = Status.FAILED,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractArbitraryCallRequestWithFailedStatusWhenTransactionHasWrongData() {
        val contractArbitraryCallRequestRepository = mock<ContractArbitraryCallRequestRepository>()
        val request = STORED_REQUEST

        suppose("contract arbitrary call request exists in the database") {
            call(contractArbitraryCallRequestRepository.getById(ID))
                .willReturn(request)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(data = FunctionData("dead"))

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val service = ContractArbitraryCallRequestServiceImpl(
            functionDecoderService = mock(),
            contractArbitraryCallRequestRepository = contractArbitraryCallRequestRepository,
            deployedContractIdentifierResolverService = service(mock()),
            contractDeploymentRequestRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = mock(),
            blacklistCheckService = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("contract arbitrary call request with failed status is returned") {
            expectThat(service.getContractArbitraryCallRequest(ID))
                .isEqualTo(
                    request.withTransactionData(
                        status = Status.FAILED,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractArbitraryCallRequestWithFailedStatusWhenTransactionHasWrongValue() {
        val contractArbitraryCallRequestRepository = mock<ContractArbitraryCallRequestRepository>()
        val request = STORED_REQUEST

        suppose("contract arbitrary call request exists in the database") {
            call(contractArbitraryCallRequestRepository.getById(ID))
                .willReturn(request)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(value = Balance(BigInteger.valueOf(123456L)))

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val service = ContractArbitraryCallRequestServiceImpl(
            functionDecoderService = mock(),
            contractArbitraryCallRequestRepository = contractArbitraryCallRequestRepository,
            deployedContractIdentifierResolverService = service(mock()),
            contractDeploymentRequestRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = mock(),
            blacklistCheckService = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("contract arbitrary call request with failed status is returned") {
            expectThat(service.getContractArbitraryCallRequest(ID))
                .isEqualTo(
                    request.withTransactionData(
                        status = Status.FAILED,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractArbitraryCallRequestWithSuccessfulStatusWhenCallerAddressIsNull() {
        val contractArbitraryCallRequestRepository = mock<ContractArbitraryCallRequestRepository>()
        val request = STORED_REQUEST.copy(callerAddress = null)

        suppose("contract arbitrary call request exists in the database") {
            call(contractArbitraryCallRequestRepository.getById(ID))
                .willReturn(request)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(from = WalletAddress("dead"))

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val service = ContractArbitraryCallRequestServiceImpl(
            functionDecoderService = mock(),
            contractArbitraryCallRequestRepository = contractArbitraryCallRequestRepository,
            deployedContractIdentifierResolverService = service(mock()),
            contractDeploymentRequestRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = mock(),
            blacklistCheckService = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("contract arbitrary call request with successful status is returned") {
            expectThat(service.getContractArbitraryCallRequest(ID))
                .isEqualTo(
                    request.withTransactionData(
                        status = Status.SUCCESS,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractArbitraryCallRequestWithSuccessfulStatusWhenCallerAddressIsNotNull() {
        val contractArbitraryCallRequestRepository = mock<ContractArbitraryCallRequestRepository>()
        val request = STORED_REQUEST

        suppose("contract arbitrary call request exists in the database") {
            call(contractArbitraryCallRequestRepository.getById(ID))
                .willReturn(request)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val service = ContractArbitraryCallRequestServiceImpl(
            functionDecoderService = mock(),
            contractArbitraryCallRequestRepository = contractArbitraryCallRequestRepository,
            deployedContractIdentifierResolverService = service(mock()),
            contractDeploymentRequestRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = mock(),
            blacklistCheckService = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(PROJECT.id),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("contract arbitrary call request with successful status is returned") {
            expectThat(service.getContractArbitraryCallRequest(ID))
                .isEqualTo(
                    request.withTransactionData(
                        status = Status.SUCCESS,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyReturnListOfContractArbitraryCallRequestsByProjectId() {
        val contractArbitraryCallRequestRepository = mock<ContractArbitraryCallRequestRepository>()
        val filters = ContractArbitraryCallRequestFilters(
            deployedContractId = ContractDeploymentRequestId(UUID.randomUUID()),
            contractAddress = ContractAddress("cafebabe")
        )

        val request = STORED_REQUEST.copy(txHash = null)

        suppose("contract arbitrary call request exists in the database") {
            call(contractArbitraryCallRequestRepository.getAllByProjectId(PROJECT.id, filters))
                .willReturn(listOf(request))
        }

        val service = ContractArbitraryCallRequestServiceImpl(
            functionDecoderService = mock(),
            contractArbitraryCallRequestRepository = contractArbitraryCallRequestRepository,
            deployedContractIdentifierResolverService = service(mock()),
            contractDeploymentRequestRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = mock(),
            blacklistCheckService = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(PROJECT.id),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("contract arbitrary call request with pending status is returned") {
            expectThat(service.getContractArbitraryCallRequestsByProjectIdAndFilters(PROJECT.id, filters))
                .isEqualTo(
                    listOf(
                        request.withTransactionData(
                            status = Status.PENDING,
                            transactionInfo = null
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyReturnEmptyListOfContractArbitraryCallRequestsForNonExistentProject() {
        val projectId = ProjectId(UUID.randomUUID())
        val filters = ContractArbitraryCallRequestFilters(
            deployedContractId = ContractDeploymentRequestId(UUID.randomUUID()),
            contractAddress = ContractAddress("cafebabe")
        )

        val service = ContractArbitraryCallRequestServiceImpl(
            functionDecoderService = mock(),
            contractArbitraryCallRequestRepository = mock(),
            deployedContractIdentifierResolverService = service(mock()),
            contractDeploymentRequestRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = mock(),
            blacklistCheckService = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(projectId),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("empty list is returned") {
            expectThat(service.getContractArbitraryCallRequestsByProjectIdAndFilters(PROJECT.id, filters))
                .isEmpty()
        }
    }

    @Test
    fun mustSuccessfullyAttachTxInfo() {
        val contractArbitraryCallRequestRepository = mock<ContractArbitraryCallRequestRepository>()
        val caller = WalletAddress("0xbc25524e0daacB1F149BA55279f593F5E3FB73e9")

        suppose("txInfo will be successfully attached to the request") {
            call(contractArbitraryCallRequestRepository.setTxInfo(ID, TX_HASH, caller))
                .willReturn(true)
        }

        val service = ContractArbitraryCallRequestServiceImpl(
            functionDecoderService = mock(),
            contractArbitraryCallRequestRepository = contractArbitraryCallRequestRepository,
            deployedContractIdentifierResolverService = service(mock()),
            contractDeploymentRequestRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = mock(),
            blacklistCheckService = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock(),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("txInfo was successfully attached") {
            service.attachTxInfo(ID, TX_HASH, caller)

            expectInteractions(contractArbitraryCallRequestRepository) {
                once.setTxInfo(ID, TX_HASH, caller)
            }
        }
    }

    @Test
    fun mustThrowCannotAttachTxInfoExceptionWhenAttachingTxInfoFails() {
        val contractArbitraryCallRequestRepository = mock<ContractArbitraryCallRequestRepository>()
        val caller = WalletAddress("0xbc25524e0daacB1F149BA55279f593F5E3FB73e9")

        suppose("attaching txInfo will fail") {
            call(contractArbitraryCallRequestRepository.setTxInfo(ID, TX_HASH, caller))
                .willReturn(false)
        }

        val service = ContractArbitraryCallRequestServiceImpl(
            functionDecoderService = mock(),
            contractArbitraryCallRequestRepository = contractArbitraryCallRequestRepository,
            deployedContractIdentifierResolverService = service(mock()),
            contractDeploymentRequestRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = mock(),
            blacklistCheckService = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock(),
            objectMapper = JsonConfig().objectMapper()
        )

        verify("CannotAttachTxInfoException is thrown") {
            expectThrows<CannotAttachTxInfoException> {
                service.attachTxInfo(ID, TX_HASH, caller)
            }

            expectInteractions(contractArbitraryCallRequestRepository) {
                once.setTxInfo(ID, TX_HASH, caller)
            }
        }
    }

    private fun projectRepositoryMock(projectId: ProjectId): ProjectRepository {
        val projectRepository = mock<ProjectRepository>()

        suppose("some project will be returned") {
            call(projectRepository.getById(projectId))
                .willReturn(
                    Project(
                        id = projectId,
                        ownerId = UserId(UUID.randomUUID()),
                        baseRedirectUrl = BaseUrl(""),
                        chainId = ChainId(0L),
                        customRpcUrl = null,
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }

        return projectRepository
    }

    private fun service(repository: ContractDeploymentRequestRepository) =
        DeployedContractIdentifierResolverServiceImpl(repository, mock())
}
