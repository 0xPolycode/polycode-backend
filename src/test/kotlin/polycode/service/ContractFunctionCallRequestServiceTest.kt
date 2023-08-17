package polycode.service

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
import polycode.features.contract.deployment.model.params.DeployedContractIdIdentifier
import polycode.features.contract.deployment.model.result.ContractDeploymentRequest
import polycode.features.contract.deployment.repository.ContractDeploymentRequestRepository
import polycode.features.contract.deployment.service.DeployedContractIdentifierResolverServiceImpl
import polycode.features.contract.functioncall.model.filters.ContractFunctionCallRequestFilters
import polycode.features.contract.functioncall.model.params.CreateContractFunctionCallRequestParams
import polycode.features.contract.functioncall.model.params.StoreContractFunctionCallRequestParams
import polycode.features.contract.functioncall.model.result.ContractFunctionCallRequest
import polycode.features.contract.functioncall.repository.ContractFunctionCallRequestRepository
import polycode.features.contract.functioncall.service.ContractFunctionCallRequestServiceImpl
import polycode.features.functions.encoding.model.FunctionArgument
import polycode.features.functions.encoding.service.FunctionEncoderService
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.generated.jooq.id.ContractFunctionCallRequestId
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
import polycode.util.JsonNodeConverter
import polycode.util.Status
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import polycode.util.WithFunctionData
import java.math.BigInteger
import java.util.UUID

class ContractFunctionCallRequestServiceTest : TestBase() {

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
        private val RAW_FUNCTION_PARAMS = JsonNodeConverter().from(
            JSON.valueOf("[{\"type\": \"string\", \"value\": \"test\"}]")
        )!!
        private val DEPLOYED_CONTRACT_ID = ContractDeploymentRequestId(UUID.randomUUID())
        private val DEPLOYED_CONTRACT_ID_CREATE_PARAMS = CreateContractFunctionCallRequestParams(
            identifier = DeployedContractIdIdentifier(DEPLOYED_CONTRACT_ID),
            functionName = "test",
            functionParams = OBJECT_MAPPER.treeToValue(RAW_FUNCTION_PARAMS, Array<FunctionArgument>::class.java)
                .toList(),
            ethAmount = Balance(BigInteger("10000")),
            redirectUrl = "redirect-url/\${id}",
            callerAddress = WalletAddress("a"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        private val CONTRACT_ADDRESS = ContractAddress("abc123")
        private val ID = ContractFunctionCallRequestId(UUID.randomUUID())
        private val ENCODED_FUNCTION_DATA = FunctionData("0x1234")
        private val STORE_PARAMS = StoreContractFunctionCallRequestParams(
            id = ID,
            deployedContractId = DEPLOYED_CONTRACT_ID,
            contractAddress = CONTRACT_ADDRESS,
            functionName = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.functionName,
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
        private val STORED_REQUEST = ContractFunctionCallRequest(
            id = ID,
            deployedContractId = DEPLOYED_CONTRACT_ID,
            contractAddress = CONTRACT_ADDRESS,
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
    fun mustCorrectlyCreateFunctionCallRequestWhenContractAddressIsNotBlacklisted() {
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

        val functionEncoderService = mock<FunctionEncoderService>()
        val createParams = DEPLOYED_CONTRACT_ID_CREATE_PARAMS

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

        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()

        suppose("contract function call request is stored in database") {
            call(contractFunctionCallRequestRepository.store(STORE_PARAMS))
                .willReturn(STORED_REQUEST)
        }

        val blacklistCheckService = mock<BlacklistCheckService>()

        suppose("contract address is not blacklisted") {
            call(blacklistCheckService.exists(CONTRACT_ADDRESS))
                .willReturn(false)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
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

        verify("contract function call request is correctly created") {
            expectThat(service.createContractFunctionCallRequest(createParams, PROJECT))
                .isEqualTo(WithFunctionData(STORED_REQUEST, ENCODED_FUNCTION_DATA))

            expectInteractions(contractFunctionCallRequestRepository) {
                once.store(STORE_PARAMS)
            }
        }
    }

    @Test
    fun mustCorrectlyCreateFunctionCallRequestWhenContractAddressIsBlacklisted() {
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

        val functionEncoderService = mock<FunctionEncoderService>()
        val createParams = DEPLOYED_CONTRACT_ID_CREATE_PARAMS

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

        val storeParams = STORE_PARAMS.copy(redirectUrl = STORE_PARAMS.redirectUrl + "/caution")
        val storedRequest = STORED_REQUEST.copy(redirectUrl = STORED_REQUEST.redirectUrl + "/caution")
        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()

        suppose("contract function call request is stored in database") {
            call(contractFunctionCallRequestRepository.store(storeParams))
                .willReturn(storedRequest)
        }

        val blacklistCheckService = mock<BlacklistCheckService>()

        suppose("contract address is not blacklisted") {
            call(blacklistCheckService.exists(CONTRACT_ADDRESS))
                .willReturn(true)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
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

        verify("contract function call request is correctly created") {
            expectThat(service.createContractFunctionCallRequest(createParams, PROJECT))
                .isEqualTo(WithFunctionData(storedRequest, ENCODED_FUNCTION_DATA))

            expectInteractions(contractFunctionCallRequestRepository) {
                once.store(storeParams)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionForNonExistentContractFunctionCallRequest() {
        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()

        suppose("contract function call request is not found in the database") {
            call(contractFunctionCallRequestRepository.getById(ID))
                .willReturn(null)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = mock(),
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
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
                service.getContractFunctionCallRequest(ID)
            }
        }
    }

    @Test
    fun mustReturnContractFunctionCallRequestWithPendingStatusWhenContractFunctionCallRequestHasNullTxHash() {
        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()
        val request = STORED_REQUEST.copy(txHash = null)

        suppose("contract function call request exists in the database") {
            call(contractFunctionCallRequestRepository.getById(ID))
                .willReturn(request)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = request.functionName,
                    arguments = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.functionParams
                )
            )
                .willReturn(ENCODED_FUNCTION_DATA)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
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

        verify("contract function call request with pending status is returned") {
            expectThat(service.getContractFunctionCallRequest(ID))
                .isEqualTo(
                    request.withTransactionAndFunctionData(
                        status = Status.PENDING,
                        data = ENCODED_FUNCTION_DATA,
                        transactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnContractFunctionCallRequestWithPendingStatusWhenTransactionIsNotYetMined() {
        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()
        val request = STORED_REQUEST

        suppose("contract function call request exists in the database") {
            call(contractFunctionCallRequestRepository.getById(ID))
                .willReturn(request)
        }

        val blockchainService = mock<BlockchainService>()

        suppose("transaction is not yet mined") {
            call(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH, EVENTS))
                .willReturn(null)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = request.functionName,
                    arguments = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.functionParams
                )
            )
                .willReturn(ENCODED_FUNCTION_DATA)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
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

        verify("contract function call request with pending status is returned") {
            expectThat(service.getContractFunctionCallRequest(ID))
                .isEqualTo(
                    request.withTransactionAndFunctionData(
                        status = Status.PENDING,
                        data = ENCODED_FUNCTION_DATA,
                        transactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnContractFunctionCallRequestWithFailedStatusWhenTransactionIsNotSuccessful() {
        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()
        val request = STORED_REQUEST

        suppose("contract function call request exists in the database") {
            call(contractFunctionCallRequestRepository.getById(ID))
                .willReturn(request)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(success = false)

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = request.functionName,
                    arguments = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.functionParams
                )
            )
                .willReturn(ENCODED_FUNCTION_DATA)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
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

        verify("contract function call request with failed status is returned") {
            expectThat(service.getContractFunctionCallRequest(ID))
                .isEqualTo(
                    request.withTransactionAndFunctionData(
                        status = Status.FAILED,
                        data = ENCODED_FUNCTION_DATA,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractFunctionCallRequestWithFailedStatusWhenTransactionHasWrongTxHash() {
        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()
        val request = STORED_REQUEST

        suppose("contract function call request exists in the database") {
            call(contractFunctionCallRequestRepository.getById(ID))
                .willReturn(request)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(hash = TransactionHash("other-tx-hash"))

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = request.functionName,
                    arguments = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.functionParams
                )
            )
                .willReturn(ENCODED_FUNCTION_DATA)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
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

        verify("contract function call request with failed status is returned") {
            expectThat(service.getContractFunctionCallRequest(ID))
                .isEqualTo(
                    request.withTransactionAndFunctionData(
                        status = Status.FAILED,
                        data = ENCODED_FUNCTION_DATA,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractFunctionCallRequestWithFailedStatusWhenTransactionHasWrongCallerAddress() {
        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()
        val request = STORED_REQUEST

        suppose("contract function call request exists in the database") {
            call(contractFunctionCallRequestRepository.getById(ID))
                .willReturn(request)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(from = WalletAddress("dead"))

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = request.functionName,
                    arguments = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.functionParams
                )
            )
                .willReturn(ENCODED_FUNCTION_DATA)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
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

        verify("contract function call request with failed status is returned") {
            expectThat(service.getContractFunctionCallRequest(ID))
                .isEqualTo(
                    request.withTransactionAndFunctionData(
                        status = Status.FAILED,
                        data = ENCODED_FUNCTION_DATA,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractFunctionCallRequestWithFailedStatusWhenTransactionHasWrongContractAddress() {
        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()
        val request = STORED_REQUEST

        suppose("contract function call request exists in the database") {
            call(contractFunctionCallRequestRepository.getById(ID))
                .willReturn(request)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(to = ContractAddress("dead"))

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = request.functionName,
                    arguments = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.functionParams
                )
            )
                .willReturn(ENCODED_FUNCTION_DATA)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
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

        verify("contract function call request with failed status is returned") {
            expectThat(service.getContractFunctionCallRequest(ID))
                .isEqualTo(
                    request.withTransactionAndFunctionData(
                        status = Status.FAILED,
                        data = ENCODED_FUNCTION_DATA,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractFunctionCallRequestWithFailedStatusWhenTransactionHasWrongData() {
        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()
        val request = STORED_REQUEST

        suppose("contract function call request exists in the database") {
            call(contractFunctionCallRequestRepository.getById(ID))
                .willReturn(request)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(data = FunctionData("dead"))

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = request.functionName,
                    arguments = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.functionParams
                )
            )
                .willReturn(ENCODED_FUNCTION_DATA)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
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

        verify("contract function call request with failed status is returned") {
            expectThat(service.getContractFunctionCallRequest(ID))
                .isEqualTo(
                    request.withTransactionAndFunctionData(
                        status = Status.FAILED,
                        data = ENCODED_FUNCTION_DATA,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractFunctionCallRequestWithFailedStatusWhenTransactionHasWrongValue() {
        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()
        val request = STORED_REQUEST

        suppose("contract function call request exists in the database") {
            call(contractFunctionCallRequestRepository.getById(ID))
                .willReturn(request)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(value = Balance(BigInteger.valueOf(123456L)))

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = request.functionName,
                    arguments = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.functionParams
                )
            )
                .willReturn(ENCODED_FUNCTION_DATA)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
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

        verify("contract function call request with failed status is returned") {
            expectThat(service.getContractFunctionCallRequest(ID))
                .isEqualTo(
                    request.withTransactionAndFunctionData(
                        status = Status.FAILED,
                        data = ENCODED_FUNCTION_DATA,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractFunctionCallRequestWithSuccessfulStatusWhenCallerAddressIsNull() {
        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()
        val request = STORED_REQUEST.copy(callerAddress = null)

        suppose("contract function call request exists in the database") {
            call(contractFunctionCallRequestRepository.getById(ID))
                .willReturn(request)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO.copy(from = WalletAddress("dead"))

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = request.functionName,
                    arguments = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.functionParams
                )
            )
                .willReturn(ENCODED_FUNCTION_DATA)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
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

        verify("contract function call request with successful status is returned") {
            expectThat(service.getContractFunctionCallRequest(ID))
                .isEqualTo(
                    request.withTransactionAndFunctionData(
                        status = Status.SUCCESS,
                        data = ENCODED_FUNCTION_DATA,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnContractFunctionCallRequestWithSuccessfulStatusWhenCallerAddressIsNotNull() {
        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()
        val request = STORED_REQUEST

        suppose("contract function call request exists in the database") {
            call(contractFunctionCallRequestRepository.getById(ID))
                .willReturn(request)
        }

        val blockchainService = mock<BlockchainService>()
        val transactionInfo = TRANSACTION_INFO

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(CHAIN_SPEC, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = request.functionName,
                    arguments = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.functionParams
                )
            )
                .willReturn(ENCODED_FUNCTION_DATA)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
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

        verify("contract function call request with successful status is returned") {
            expectThat(service.getContractFunctionCallRequest(ID))
                .isEqualTo(
                    request.withTransactionAndFunctionData(
                        status = Status.SUCCESS,
                        data = ENCODED_FUNCTION_DATA,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyReturnListOfContractFunctionCallRequestsByProjectId() {
        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()
        val filters = ContractFunctionCallRequestFilters(
            deployedContractId = ContractDeploymentRequestId(UUID.randomUUID()),
            contractAddress = ContractAddress("cafebabe")
        )

        val request = STORED_REQUEST.copy(txHash = null)

        suppose("contract function call request exists in the database") {
            call(contractFunctionCallRequestRepository.getAllByProjectId(PROJECT.id, filters))
                .willReturn(listOf(request))
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = request.functionName,
                    arguments = DEPLOYED_CONTRACT_ID_CREATE_PARAMS.functionParams
                )
            )
                .willReturn(ENCODED_FUNCTION_DATA)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
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

        verify("contract function call request with pending status is returned") {
            expectThat(service.getContractFunctionCallRequestsByProjectIdAndFilters(PROJECT.id, filters))
                .isEqualTo(
                    listOf(
                        request.withTransactionAndFunctionData(
                            status = Status.PENDING,
                            data = ENCODED_FUNCTION_DATA,
                            transactionInfo = null
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyReturnEmptyListOfContractFunctionCallRequestsForNonExistentProject() {
        val projectId = ProjectId(UUID.randomUUID())
        val filters = ContractFunctionCallRequestFilters(
            deployedContractId = ContractDeploymentRequestId(UUID.randomUUID()),
            contractAddress = ContractAddress("cafebabe")
        )

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = mock(),
            contractFunctionCallRequestRepository = mock(),
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
            expectThat(service.getContractFunctionCallRequestsByProjectIdAndFilters(PROJECT.id, filters))
                .isEmpty()
        }
    }

    @Test
    fun mustSuccessfullyAttachTxInfo() {
        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()
        val caller = WalletAddress("0xbc25524e0daacB1F149BA55279f593F5E3FB73e9")

        suppose("txInfo will be successfully attached to the request") {
            call(contractFunctionCallRequestRepository.setTxInfo(ID, TX_HASH, caller))
                .willReturn(true)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = mock(),
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
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

            expectInteractions(contractFunctionCallRequestRepository) {
                once.setTxInfo(ID, TX_HASH, caller)
            }
        }
    }

    @Test
    fun mustThrowCannotAttachTxInfoExceptionWhenAttachingTxInfoFails() {
        val contractFunctionCallRequestRepository = mock<ContractFunctionCallRequestRepository>()
        val caller = WalletAddress("0xbc25524e0daacB1F149BA55279f593F5E3FB73e9")

        suppose("attaching txInfo will fail") {
            call(contractFunctionCallRequestRepository.setTxInfo(ID, TX_HASH, caller))
                .willReturn(false)
        }

        val service = ContractFunctionCallRequestServiceImpl(
            functionEncoderService = mock(),
            contractFunctionCallRequestRepository = contractFunctionCallRequestRepository,
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

            expectInteractions(contractFunctionCallRequestRepository) {
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
