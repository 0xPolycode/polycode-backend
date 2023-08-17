package polycode.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import polycode.TestBase
import polycode.TestData
import polycode.blockchain.BlockchainService
import polycode.blockchain.properties.ChainSpec
import polycode.exception.CannotAttachTxInfoException
import polycode.exception.ResourceNotFoundException
import polycode.features.api.access.model.result.Project
import polycode.features.api.access.repository.ProjectRepository
import polycode.features.asset.lock.model.params.CreateErc20LockRequestParams
import polycode.features.asset.lock.model.params.StoreErc20LockRequestParams
import polycode.features.asset.lock.model.result.Erc20LockRequest
import polycode.features.asset.lock.repository.Erc20LockRequestRepository
import polycode.features.asset.lock.service.Erc20LockRequestServiceImpl
import polycode.features.functions.encoding.model.FunctionArgument
import polycode.features.functions.encoding.service.FunctionEncoderService
import polycode.generated.jooq.id.Erc20LockRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.model.DeserializableEvent
import polycode.model.ScreenConfig
import polycode.model.result.BlockchainTransactionInfo
import polycode.util.Balance
import polycode.util.BaseUrl
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.DurationSeconds
import polycode.util.FunctionData
import polycode.util.Status
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import polycode.util.WithFunctionData
import polycode.util.ZeroAddress
import java.math.BigInteger
import java.util.UUID

class Erc20LockRequestServiceTest : TestBase() {

    companion object {
        private val PROJECT = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = UserId(UUID.randomUUID()),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = ChainId(1337L),
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        private val CREATE_PARAMS = CreateErc20LockRequestParams(
            redirectUrl = "redirect-url/\${id}",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.valueOf(123456L)),
            lockDuration = DurationSeconds(BigInteger.valueOf(123L)),
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = WalletAddress("c"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        private val TX_HASH = TransactionHash("tx-hash")
        private val EVENTS = listOf<DeserializableEvent>()
    }

    @Test
    fun mustSuccessfullyCreateErc20LockRequest() {
        val uuidProvider = mock<UuidProvider>()
        val id = Erc20LockRequestId(UUID.randomUUID())

        suppose("some UUID will be generated") {
            call(uuidProvider.getRawUuid())
                .willReturn(id.value)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val functionEncoderService = mock<FunctionEncoderService>()
        val encodedData = FunctionData("encoded")

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.tokenAddress),
                        FunctionArgument(CREATE_PARAMS.tokenAmount),
                        FunctionArgument(CREATE_PARAMS.lockDuration),
                        FunctionArgument(id.value.toString()),
                        FunctionArgument(ZeroAddress)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()
        val redirectUrl = CREATE_PARAMS.redirectUrl!!

        val storeParams = StoreErc20LockRequestParams(
            id = id,
            projectId = PROJECT.id,
            chainId = PROJECT.chainId,
            redirectUrl = redirectUrl.replace("\${id}", id.value.toString()),
            tokenAddress = CREATE_PARAMS.tokenAddress,
            tokenAmount = CREATE_PARAMS.tokenAmount,
            lockDuration = CREATE_PARAMS.lockDuration,
            lockContractAddress = CREATE_PARAMS.lockContractAddress,
            tokenSenderAddress = CREATE_PARAMS.tokenSenderAddress,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            screenConfig = CREATE_PARAMS.screenConfig,
            createdAt = TestData.TIMESTAMP
        )

        val storedRequest = Erc20LockRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = PROJECT.chainId,
            redirectUrl = storeParams.redirectUrl,
            tokenAddress = CREATE_PARAMS.tokenAddress,
            tokenAmount = CREATE_PARAMS.tokenAmount,
            lockDuration = CREATE_PARAMS.lockDuration,
            lockContractAddress = CREATE_PARAMS.lockContractAddress,
            tokenSenderAddress = CREATE_PARAMS.tokenSenderAddress,
            txHash = null,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            screenConfig = CREATE_PARAMS.screenConfig,
            createdAt = TestData.TIMESTAMP
        )

        suppose("ERC20 lock request is stored in database") {
            call(erc20LockRequestRepository.store(storeParams))
                .willReturn(storedRequest)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = uuidProvider,
                utcDateTimeProvider = utcDateTimeProvider,
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("ERC20 lock request is correctly created") {
            expectThat(service.createErc20LockRequest(CREATE_PARAMS, PROJECT))
                .isEqualTo(WithFunctionData(storedRequest, encodedData))

            expectInteractions(erc20LockRequestRepository) {
                once.store(storeParams)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionForNonExistentErc20LockRequest() {
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request does not exist in database") {
            call(erc20LockRequestRepository.getById(anyValueClass(Erc20LockRequestId(UUID.randomUUID()))))
                .willReturn(null)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = mock(),
            erc20LockRequestRepository = erc20LockRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.getErc20LockRequest(id = Erc20LockRequestId(UUID.randomUUID()))
            }
        }
    }

    @Test
    fun mustReturnErc20LockRequestWithPendingStatusWhenErc20LockRequestHasNullTxHash() {
        val id = Erc20LockRequestId(UUID.randomUUID())
        val lockRequest = Erc20LockRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            lockDuration = CREATE_PARAMS.lockDuration,
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = WalletAddress("c"),
            txHash = null,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request exists in database") {
            call(erc20LockRequestRepository.getById(id))
                .willReturn(lockRequest)
        }

        val functionEncoderService = mock<FunctionEncoderService>()
        val encodedData = FunctionData("encoded")

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(lockRequest.tokenAddress),
                        FunctionArgument(lockRequest.tokenAmount),
                        FunctionArgument(lockRequest.lockDuration),
                        FunctionArgument(id.value.toString()),
                        FunctionArgument(ZeroAddress)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(lockRequest.projectId)
        )

        verify("ERC20 lock request with pending status is returned") {
            expectThat(service.getErc20LockRequest(id))
                .isEqualTo(
                    lockRequest.withTransactionData(
                        status = Status.PENDING,
                        data = encodedData,
                        transactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20LockRequestWithPendingStatusWhenTransactionIsNotYetMined() {
        val id = Erc20LockRequestId(UUID.randomUUID())
        val lockRequest = Erc20LockRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            lockDuration = CREATE_PARAMS.lockDuration,
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request exists in database") {
            call(erc20LockRequestRepository.getById(id))
                .willReturn(lockRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(lockRequest.chainId, null)

        suppose("transaction is not yet mined") {
            call(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, EVENTS))
                .willReturn(null)
        }

        val functionEncoderService = mock<FunctionEncoderService>()
        val encodedData = FunctionData("encoded")

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(lockRequest.tokenAddress),
                        FunctionArgument(lockRequest.tokenAmount),
                        FunctionArgument(lockRequest.lockDuration),
                        FunctionArgument(id.value.toString()),
                        FunctionArgument(ZeroAddress)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(lockRequest.projectId)
        )

        verify("ERC20 lock request with pending status is returned") {
            expectThat(service.getErc20LockRequest(id))
                .isEqualTo(
                    lockRequest.withTransactionData(
                        status = Status.PENDING,
                        data = encodedData,
                        transactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20LockRequestWithFailedStatusWhenTransactionIsNotSuccessful() {
        val id = Erc20LockRequestId(UUID.randomUUID())
        val lockRequest = Erc20LockRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            lockDuration = CREATE_PARAMS.lockDuration,
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request exists in database") {
            call(erc20LockRequestRepository.getById(id))
                .willReturn(lockRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(lockRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = lockRequest.tokenSenderAddress!!,
            to = lockRequest.lockContractAddress,
            deployedContractAddress = null,
            data = encodedData,
            value = Balance.ZERO,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = false,
            events = emptyList()
        )

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(lockRequest.tokenAddress),
                        FunctionArgument(lockRequest.tokenAmount),
                        FunctionArgument(lockRequest.lockDuration),
                        FunctionArgument(id.value.toString()),
                        FunctionArgument(ZeroAddress)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(lockRequest.projectId)
        )

        verify("ERC20 lock request with failed status is returned") {
            expectThat(service.getErc20LockRequest(id))
                .isEqualTo(
                    lockRequest.withTransactionData(
                        status = Status.FAILED,
                        data = encodedData,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20LockRequestWithFailedStatusWhenTransactionHasWrongToAddress() {
        val id = Erc20LockRequestId(UUID.randomUUID())
        val lockRequest = Erc20LockRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            lockDuration = CREATE_PARAMS.lockDuration,
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request exists in database") {
            call(erc20LockRequestRepository.getById(id))
                .willReturn(lockRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(lockRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = lockRequest.tokenSenderAddress!!,
            to = WalletAddress("dead"),
            deployedContractAddress = null,
            data = encodedData,
            value = Balance.ZERO,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(lockRequest.tokenAddress),
                        FunctionArgument(lockRequest.tokenAmount),
                        FunctionArgument(lockRequest.lockDuration),
                        FunctionArgument(id.value.toString()),
                        FunctionArgument(ZeroAddress)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(lockRequest.projectId)
        )

        verify("ERC20 lock request with failed status is returned") {
            expectThat(service.getErc20LockRequest(id))
                .isEqualTo(
                    lockRequest.withTransactionData(
                        status = Status.FAILED,
                        data = encodedData,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20LockRequestWithFailedStatusWhenTransactionHasWrongTxHash() {
        val id = Erc20LockRequestId(UUID.randomUUID())
        val lockRequest = Erc20LockRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            lockDuration = CREATE_PARAMS.lockDuration,
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request exists in database") {
            call(erc20LockRequestRepository.getById(id))
                .willReturn(lockRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(lockRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TransactionHash("wrong-hash"),
            from = lockRequest.tokenSenderAddress!!,
            to = lockRequest.lockContractAddress,
            deployedContractAddress = null,
            data = encodedData,
            value = Balance.ZERO,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(lockRequest.tokenAddress),
                        FunctionArgument(lockRequest.tokenAmount),
                        FunctionArgument(lockRequest.lockDuration),
                        FunctionArgument(id.value.toString()),
                        FunctionArgument(ZeroAddress)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(lockRequest.projectId)
        )

        verify("ERC20 lock request with failed status is returned") {
            expectThat(service.getErc20LockRequest(id))
                .isEqualTo(
                    lockRequest.withTransactionData(
                        status = Status.FAILED,
                        data = encodedData,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20LockRequestWithFailedStatusWhenTransactionHasWrongFromAddress() {
        val id = Erc20LockRequestId(UUID.randomUUID())
        val lockRequest = Erc20LockRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            lockDuration = CREATE_PARAMS.lockDuration,
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request exists in database") {
            call(erc20LockRequestRepository.getById(id))
                .willReturn(lockRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(lockRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = WalletAddress("dead"),
            to = lockRequest.lockContractAddress,
            deployedContractAddress = null,
            data = encodedData,
            value = Balance.ZERO,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(lockRequest.tokenAddress),
                        FunctionArgument(lockRequest.tokenAmount),
                        FunctionArgument(lockRequest.lockDuration),
                        FunctionArgument(id.value.toString()),
                        FunctionArgument(ZeroAddress)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(lockRequest.projectId)
        )

        verify("ERC20 lock request with failed status is returned") {
            expectThat(service.getErc20LockRequest(id))
                .isEqualTo(
                    lockRequest.withTransactionData(
                        status = Status.FAILED,
                        data = encodedData,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20LockRequestWithFailedStatusWhenTransactionHasWrongData() {
        val id = Erc20LockRequestId(UUID.randomUUID())
        val lockRequest = Erc20LockRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            lockDuration = CREATE_PARAMS.lockDuration,
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request exists in database") {
            call(erc20LockRequestRepository.getById(id))
                .willReturn(lockRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(lockRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = lockRequest.tokenSenderAddress!!,
            to = lockRequest.lockContractAddress,
            deployedContractAddress = null,
            data = FunctionData("wrong-data"),
            value = Balance.ZERO,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(lockRequest.tokenAddress),
                        FunctionArgument(lockRequest.tokenAmount),
                        FunctionArgument(lockRequest.lockDuration),
                        FunctionArgument(id.value.toString()),
                        FunctionArgument(ZeroAddress)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(lockRequest.projectId)
        )

        verify("ERC20 lock request with failed status is returned") {
            expectThat(service.getErc20LockRequest(id))
                .isEqualTo(
                    lockRequest.withTransactionData(
                        status = Status.FAILED,
                        data = encodedData,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20LockRequestWithSuccessfulStatusWhenFromAddressIsNull() {
        val id = Erc20LockRequestId(UUID.randomUUID())
        val lockRequest = Erc20LockRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            lockDuration = CREATE_PARAMS.lockDuration,
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = null,
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request exists in database") {
            call(erc20LockRequestRepository.getById(id))
                .willReturn(lockRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(lockRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = WalletAddress("0cafe0babe"),
            to = lockRequest.lockContractAddress,
            deployedContractAddress = null,
            data = encodedData,
            value = Balance.ZERO,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(lockRequest.tokenAddress),
                        FunctionArgument(lockRequest.tokenAmount),
                        FunctionArgument(lockRequest.lockDuration),
                        FunctionArgument(id.value.toString()),
                        FunctionArgument(ZeroAddress)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(lockRequest.projectId)
        )

        verify("ERC20 lock request with successful status is returned") {
            expectThat(service.getErc20LockRequest(id))
                .isEqualTo(
                    lockRequest.withTransactionData(
                        status = Status.SUCCESS,
                        data = encodedData,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnErc20LockRequestWithSuccessfulStatusWhenFromAddressIsSpecified() {
        val id = Erc20LockRequestId(UUID.randomUUID())
        val lockRequest = Erc20LockRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            lockDuration = CREATE_PARAMS.lockDuration,
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request exists in database") {
            call(erc20LockRequestRepository.getById(id))
                .willReturn(lockRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(lockRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = lockRequest.tokenSenderAddress!!,
            to = lockRequest.lockContractAddress,
            deployedContractAddress = null,
            data = encodedData,
            value = Balance.ZERO,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(lockRequest.tokenAddress),
                        FunctionArgument(lockRequest.tokenAmount),
                        FunctionArgument(lockRequest.lockDuration),
                        FunctionArgument(id.value.toString()),
                        FunctionArgument(ZeroAddress)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(lockRequest.projectId)
        )

        verify("ERC20 lock request with successful status is returned") {
            expectThat(service.getErc20LockRequest(id))
                .isEqualTo(
                    lockRequest.withTransactionData(
                        status = Status.SUCCESS,
                        data = encodedData,
                        transactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyReturnListOfErc20LockRequestsByProjectId() {
        val id = Erc20LockRequestId(UUID.randomUUID())
        val lockRequest = Erc20LockRequest(
            id = id,
            projectId = PROJECT.id,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "test",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            lockDuration = CREATE_PARAMS.lockDuration,
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = WalletAddress("c"),
            txHash = TX_HASH,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            createdAt = TestData.TIMESTAMP
        )
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()

        suppose("ERC20 lock request exists in database") {
            call(erc20LockRequestRepository.getAllByProjectId(PROJECT.id))
                .willReturn(listOf(lockRequest))
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(lockRequest.chainId, null)
        val encodedData = FunctionData("encoded")
        val transactionInfo = BlockchainTransactionInfo(
            hash = TX_HASH,
            from = lockRequest.tokenSenderAddress!!,
            to = lockRequest.lockContractAddress,
            deployedContractAddress = null,
            data = encodedData,
            value = Balance.ZERO,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )

        suppose("transaction is mined") {
            call(blockchainService.fetchTransactionInfo(chainSpec, TX_HASH, EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "lock",
                    arguments = listOf(
                        FunctionArgument(lockRequest.tokenAddress),
                        FunctionArgument(lockRequest.tokenAmount),
                        FunctionArgument(lockRequest.lockDuration),
                        FunctionArgument(id.value.toString()),
                        FunctionArgument(ZeroAddress)
                    )
                )
            )
                .willReturn(encodedData)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            erc20LockRequestRepository = erc20LockRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(lockRequest.projectId)
        )

        verify("ERC20 lock request with successful status is returned") {
            expectThat(service.getErc20LockRequestsByProjectId(PROJECT.id))
                .isEqualTo(
                    listOf(
                        lockRequest.withTransactionData(
                            status = Status.SUCCESS,
                            data = encodedData,
                            transactionInfo = transactionInfo
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyReturnEmptyListOfErc20LockRequestsForNonExistentProject() {
        val projectId = ProjectId(UUID.randomUUID())
        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = mock(),
            erc20LockRequestRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(projectId)
        )

        verify("empty list is returned") {
            val result = service.getErc20LockRequestsByProjectId(projectId)

            expectThat(result)
                .isEmpty()
        }
    }

    @Test
    fun mustSuccessfullyAttachTxInfo() {
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()
        val id = Erc20LockRequestId(UUID.randomUUID())
        val caller = WalletAddress("0xbc25524e0daacB1F149BA55279f593F5E3FB73e9")

        suppose("txInfo will be successfully attached to the request") {
            call(erc20LockRequestRepository.setTxInfo(id, TX_HASH, caller))
                .willReturn(true)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = mock(),
            erc20LockRequestRepository = erc20LockRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("txInfo was successfully attached") {
            service.attachTxInfo(id, TX_HASH, caller)

            expectInteractions(erc20LockRequestRepository) {
                once.setTxInfo(id, TX_HASH, caller)
            }
        }
    }

    @Test
    fun mustThrowCannotAttachTxInfoExceptionWhenAttachingTxInfoFails() {
        val erc20LockRequestRepository = mock<Erc20LockRequestRepository>()
        val id = Erc20LockRequestId(UUID.randomUUID())
        val caller = WalletAddress("0xbc25524e0daacB1F149BA55279f593F5E3FB73e9")

        suppose("attaching txInfo will fail") {
            call(erc20LockRequestRepository.setTxInfo(id, TX_HASH, caller))
                .willReturn(false)
        }

        val service = Erc20LockRequestServiceImpl(
            functionEncoderService = mock(),
            erc20LockRequestRepository = erc20LockRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("CannotAttachTxInfoException is thrown") {
            expectThrows<CannotAttachTxInfoException> {
                service.attachTxInfo(id, TX_HASH, caller)
            }

            expectInteractions(erc20LockRequestRepository) {
                once.setTxInfo(id, TX_HASH, caller)
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
}
