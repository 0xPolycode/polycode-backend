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
import polycode.features.asset.multisend.model.params.CreateAssetMultiSendRequestParams
import polycode.features.asset.multisend.model.params.StoreAssetMultiSendRequestParams
import polycode.features.asset.multisend.model.result.AssetMultiSendRequest
import polycode.features.asset.multisend.repository.AssetMultiSendRequestRepository
import polycode.features.asset.multisend.service.AssetMultiSendRequestServiceImpl
import polycode.features.functions.encoding.model.FunctionArgument
import polycode.features.functions.encoding.service.FunctionEncoderService
import polycode.generated.jooq.id.AssetMultiSendRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.model.ScreenConfig
import polycode.model.result.BlockchainTransactionInfo
import polycode.util.Balance
import polycode.util.BaseUrl
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.FunctionData
import polycode.util.PredefinedEvents
import polycode.util.Status
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import polycode.util.WithFunctionDataOrEthValue
import java.math.BigInteger
import java.util.UUID

class AssetMultiSendRequestServiceTest : TestBase() {

    companion object {
        private val PROJECT = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = UserId(UUID.randomUUID()),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = ChainId(1337L),
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        private val CREATE_PARAMS = CreateAssetMultiSendRequestParams(
            redirectUrl = "redirect-url/\${id}",
            tokenAddress = ContractAddress("a"),
            disperseContractAddress = ContractAddress("b"),
            assetAmounts = listOf(Balance(BigInteger.valueOf(123456L)), Balance(BigInteger.valueOf(789L))),
            assetRecipientAddresses = listOf(WalletAddress("c"), WalletAddress("d")),
            itemNames = listOf("item1", null),
            assetSenderAddress = WalletAddress("e"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            approveScreenConfig = ScreenConfig(
                beforeActionMessage = "approve-before-action-message",
                afterActionMessage = "approve-after-action-message"
            ),
            disperseScreenConfig = ScreenConfig(
                beforeActionMessage = "disperse-before-action-message",
                afterActionMessage = "disperse-after-action-message"
            )
        )
        private val TOTAL_TOKEN_AMOUNT = Balance(CREATE_PARAMS.assetAmounts.sumOf { it.rawValue })
        private val APPROVE_TX_HASH = TransactionHash("approve-tx-hash")
        private val DISPERSE_TX_HASH = TransactionHash("disperse-tx-hash")
        private val ID = AssetMultiSendRequestId(UUID.randomUUID())
        private val STORE_PARAMS = StoreAssetMultiSendRequestParams(
            id = ID,
            projectId = PROJECT.id,
            chainId = PROJECT.chainId,
            redirectUrl = CREATE_PARAMS.redirectUrl!!.replace("\${id}", ID.value.toString()),
            tokenAddress = CREATE_PARAMS.tokenAddress,
            disperseContractAddress = CREATE_PARAMS.disperseContractAddress,
            assetAmounts = CREATE_PARAMS.assetAmounts,
            assetRecipientAddresses = CREATE_PARAMS.assetRecipientAddresses,
            itemNames = CREATE_PARAMS.itemNames,
            assetSenderAddress = CREATE_PARAMS.assetSenderAddress,
            arbitraryData = CREATE_PARAMS.arbitraryData,
            approveScreenConfig = CREATE_PARAMS.approveScreenConfig,
            disperseScreenConfig = CREATE_PARAMS.disperseScreenConfig,
            createdAt = TestData.TIMESTAMP
        )
        private val STORED_REQUEST = AssetMultiSendRequest(
            id = ID,
            projectId = PROJECT.id,
            chainId = PROJECT.chainId,
            redirectUrl = STORE_PARAMS.redirectUrl,
            tokenAddress = STORE_PARAMS.tokenAddress,
            disperseContractAddress = STORE_PARAMS.disperseContractAddress,
            assetAmounts = STORE_PARAMS.assetAmounts,
            assetRecipientAddresses = STORE_PARAMS.assetRecipientAddresses,
            itemNames = STORE_PARAMS.itemNames,
            assetSenderAddress = STORE_PARAMS.assetSenderAddress,
            approveTxHash = null,
            disperseTxHash = null,
            arbitraryData = STORE_PARAMS.arbitraryData,
            approveScreenConfig = STORE_PARAMS.approveScreenConfig,
            disperseScreenConfig = STORE_PARAMS.disperseScreenConfig,
            createdAt = TestData.TIMESTAMP
        )
        private val ENCODED_APPROVE_DATA = FunctionData("encoded-approve")
        private val APPROVE_TX_INFO = BlockchainTransactionInfo(
            hash = APPROVE_TX_HASH,
            from = STORED_REQUEST.assetSenderAddress!!,
            to = STORED_REQUEST.tokenAddress!!,
            deployedContractAddress = null,
            data = ENCODED_APPROVE_DATA,
            value = Balance.ZERO,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )
        private val ENCODED_DISPERSE_ETHER_DATA = FunctionData("encoded-disperse-ether")
        private val DISPERSE_ETHER_TX_INFO = BlockchainTransactionInfo(
            hash = DISPERSE_TX_HASH,
            from = STORED_REQUEST.assetSenderAddress!!,
            to = STORED_REQUEST.disperseContractAddress,
            deployedContractAddress = null,
            data = ENCODED_DISPERSE_ETHER_DATA,
            value = TOTAL_TOKEN_AMOUNT,
            blockConfirmations = BigInteger.ONE,
            timestamp = TestData.TIMESTAMP,
            success = true,
            events = emptyList()
        )
        private val ENCODED_DISPERSE_TOKEN_DATA = FunctionData("encoded-disperse-token")
        private val DISPERSE_TOKEN_TX_INFO = DISPERSE_ETHER_TX_INFO.copy(
            data = ENCODED_DISPERSE_TOKEN_DATA,
            value = Balance.ZERO
        )
        private val APPROVAL_EVENTS = listOf(PredefinedEvents.ERC20_APPROVAL)
        private val TRANSFER_EVENTS = listOf(PredefinedEvents.ERC20_TRANSFER)
    }

    @Test
    fun mustSuccessfullyCreateAssetMultiSendRequestForSomeToken() {
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

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "approve",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.disperseContractAddress),
                        FunctionArgument(TOTAL_TOKEN_AMOUNT)
                    )
                )
            )
                .willReturn(ENCODED_APPROVE_DATA)
        }

        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()

        suppose("asset multi-send request is stored in database") {
            call(assetMultiSendRequestRepository.store(STORE_PARAMS))
                .willReturn(STORED_REQUEST)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = uuidProvider,
                utcDateTimeProvider = utcDateTimeProvider,
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("asset multi-send request is correctly created") {
            expectThat(service.createAssetMultiSendRequest(CREATE_PARAMS, PROJECT))
                .isEqualTo(WithFunctionDataOrEthValue(STORED_REQUEST, ENCODED_APPROVE_DATA, null))

            expectInteractions(assetMultiSendRequestRepository) {
                once.store(STORE_PARAMS)
            }
        }
    }

    @Test
    fun mustSuccessfullyCreateAssetMultiSendRequestForNativeAsset() {
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

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "disperseEther",
                    arguments = listOf(
                        FunctionArgument.fromAddresses(CREATE_PARAMS.assetRecipientAddresses),
                        FunctionArgument.fromUint256s(CREATE_PARAMS.assetAmounts)
                    )
                )
            )
                .willReturn(ENCODED_DISPERSE_ETHER_DATA)
        }

        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()

        val storeParams = STORE_PARAMS.copy(tokenAddress = null)
        val storedRequest = STORED_REQUEST.copy(tokenAddress = null)

        suppose("asset multi-send request is stored in database") {
            call(assetMultiSendRequestRepository.store(storeParams))
                .willReturn(storedRequest)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = uuidProvider,
                utcDateTimeProvider = utcDateTimeProvider,
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("asset multi-send request is correctly created") {
            expectThat(service.createAssetMultiSendRequest(CREATE_PARAMS.copy(tokenAddress = null), PROJECT))
                .isEqualTo(WithFunctionDataOrEthValue(storedRequest, ENCODED_DISPERSE_ETHER_DATA, TOTAL_TOKEN_AMOUNT))

            expectInteractions(assetMultiSendRequestRepository) {
                once.store(storeParams)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionForNonExistentAssetMultiSendRequest() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()

        suppose("asset multi-send request does not exist in database") {
            call(assetMultiSendRequestRepository.getById(anyValueClass(AssetMultiSendRequestId(UUID.randomUUID()))))
                .willReturn(null)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = mock(),
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.getAssetMultiSendRequest(id = AssetMultiSendRequestId(UUID.randomUUID()))
            }
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithPendingApproveStatusWhenAssetMultiSendRequestHasNullApproveTxHash() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()

        suppose("asset multi-send request exists in database") {
            call(assetMultiSendRequestRepository.getById(ID))
                .willReturn(STORED_REQUEST)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "approve",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.disperseContractAddress),
                        FunctionArgument(TOTAL_TOKEN_AMOUNT)
                    )
                )
            )
                .willReturn(ENCODED_APPROVE_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(STORED_REQUEST.projectId)
        )

        verify("asset multi-send request with pending approve status is returned") {
            expectThat(service.getAssetMultiSendRequest(ID))
                .isEqualTo(
                    STORED_REQUEST.withMultiTransactionData(
                        approveStatus = Status.PENDING,
                        approveData = ENCODED_APPROVE_DATA,
                        approveTransactionInfo = null,
                        disperseStatus = null,
                        disperseData = null,
                        disperseValue = null,
                        disperseTransactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithPendingDisperseStatusWhenAssetMultiSendRequestHasNullDisperseTxHash() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(tokenAddress = null)

        suppose("asset multi-send request exists in database") {
            call(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "disperseEther",
                    arguments = listOf(
                        FunctionArgument.fromAddresses(CREATE_PARAMS.assetRecipientAddresses),
                        FunctionArgument.fromUint256s(CREATE_PARAMS.assetAmounts)
                    )
                )
            )
                .willReturn(ENCODED_DISPERSE_ETHER_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with pending disperse status is returned") {
            expectThat(service.getAssetMultiSendRequest(ID))
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = null,
                        approveData = null,
                        approveTransactionInfo = null,
                        disperseStatus = Status.PENDING,
                        disperseData = ENCODED_DISPERSE_ETHER_DATA,
                        disperseValue = TOTAL_TOKEN_AMOUNT,
                        disperseTransactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithPendingApproveStatusWhenApproveTransactionIsNotYetMined() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(approveTxHash = APPROVE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            call(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)

        suppose("transaction is not yet mined") {
            call(blockchainService.fetchTransactionInfo(chainSpec, APPROVE_TX_HASH, APPROVAL_EVENTS))
                .willReturn(null)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "approve",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.disperseContractAddress),
                        FunctionArgument(TOTAL_TOKEN_AMOUNT)
                    )
                )
            )
                .willReturn(ENCODED_APPROVE_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with pending approve status is returned") {
            expectThat(service.getAssetMultiSendRequest(ID))
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = Status.PENDING,
                        approveData = ENCODED_APPROVE_DATA,
                        approveTransactionInfo = null,
                        disperseStatus = null,
                        disperseData = null,
                        disperseValue = null,
                        disperseTransactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithPendingDisperseStatusWhenDisperseTransactionIsNotYetMined() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(tokenAddress = null, disperseTxHash = DISPERSE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            call(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)

        suppose("transaction is not yet mined") {
            call(blockchainService.fetchTransactionInfo(chainSpec, DISPERSE_TX_HASH, TRANSFER_EVENTS))
                .willReturn(null)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "disperseEther",
                    arguments = listOf(
                        FunctionArgument.fromAddresses(CREATE_PARAMS.assetRecipientAddresses),
                        FunctionArgument.fromUint256s(CREATE_PARAMS.assetAmounts)
                    )
                )
            )
                .willReturn(ENCODED_DISPERSE_ETHER_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with pending disperse status is returned") {
            expectThat(service.getAssetMultiSendRequest(ID))
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = null,
                        approveData = null,
                        approveTransactionInfo = null,
                        disperseStatus = Status.PENDING,
                        disperseData = ENCODED_DISPERSE_ETHER_DATA,
                        disperseValue = TOTAL_TOKEN_AMOUNT,
                        disperseTransactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithFailedApproveStatusWhenApproveTransactionIsNotSuccessful() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(approveTxHash = APPROVE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            call(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = APPROVE_TX_INFO.copy(success = false)

        suppose("transaction is returned") {
            call(blockchainService.fetchTransactionInfo(chainSpec, APPROVE_TX_HASH, APPROVAL_EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "approve",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.disperseContractAddress),
                        FunctionArgument(TOTAL_TOKEN_AMOUNT)
                    )
                )
            )
                .willReturn(ENCODED_APPROVE_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with failed approve status is returned") {
            expectThat(service.getAssetMultiSendRequest(ID))
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = Status.FAILED,
                        approveData = ENCODED_APPROVE_DATA,
                        approveTransactionInfo = transactionInfo,
                        disperseStatus = null,
                        disperseData = null,
                        disperseValue = null,
                        disperseTransactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithFailedDisperseStatusWhenDisperseTransactionIsNotSuccessful() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(tokenAddress = null, disperseTxHash = DISPERSE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            call(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = DISPERSE_ETHER_TX_INFO.copy(success = false)

        suppose("transaction is returned") {
            call(blockchainService.fetchTransactionInfo(chainSpec, DISPERSE_TX_HASH, TRANSFER_EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "disperseEther",
                    arguments = listOf(
                        FunctionArgument.fromAddresses(CREATE_PARAMS.assetRecipientAddresses),
                        FunctionArgument.fromUint256s(CREATE_PARAMS.assetAmounts)
                    )
                )
            )
                .willReturn(ENCODED_DISPERSE_ETHER_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with failed disperse status is returned") {
            expectThat(service.getAssetMultiSendRequest(ID))
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = null,
                        approveData = null,
                        approveTransactionInfo = null,
                        disperseStatus = Status.FAILED,
                        disperseData = ENCODED_DISPERSE_ETHER_DATA,
                        disperseValue = TOTAL_TOKEN_AMOUNT,
                        disperseTransactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithFailedApproveStatusWhenApproveTransactionHasWrongTxHash() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(approveTxHash = APPROVE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            call(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = APPROVE_TX_INFO.copy(hash = TransactionHash("wrong"))

        suppose("transaction is returned") {
            call(blockchainService.fetchTransactionInfo(chainSpec, APPROVE_TX_HASH, APPROVAL_EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "approve",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.disperseContractAddress),
                        FunctionArgument(TOTAL_TOKEN_AMOUNT)
                    )
                )
            )
                .willReturn(ENCODED_APPROVE_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with failed approve status is returned") {
            expectThat(service.getAssetMultiSendRequest(ID))
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = Status.FAILED,
                        approveData = ENCODED_APPROVE_DATA,
                        approveTransactionInfo = transactionInfo,
                        disperseStatus = null,
                        disperseData = null,
                        disperseValue = null,
                        disperseTransactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithFailedDisperseStatusWhenDisperseTransactionHasWrongTxHash() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(tokenAddress = null, disperseTxHash = DISPERSE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            call(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = DISPERSE_ETHER_TX_INFO.copy(hash = TransactionHash("wrong"))

        suppose("transaction is returned") {
            call(blockchainService.fetchTransactionInfo(chainSpec, DISPERSE_TX_HASH, TRANSFER_EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "disperseEther",
                    arguments = listOf(
                        FunctionArgument.fromAddresses(CREATE_PARAMS.assetRecipientAddresses),
                        FunctionArgument.fromUint256s(CREATE_PARAMS.assetAmounts)
                    )
                )
            )
                .willReturn(ENCODED_DISPERSE_ETHER_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with failed disperse status is returned") {
            expectThat(service.getAssetMultiSendRequest(ID))
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = null,
                        approveData = null,
                        approveTransactionInfo = null,
                        disperseStatus = Status.FAILED,
                        disperseData = ENCODED_DISPERSE_ETHER_DATA,
                        disperseValue = TOTAL_TOKEN_AMOUNT,
                        disperseTransactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithFailedApproveStatusWhenApproveTransactionHasMismatchingFromAddress() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(approveTxHash = APPROVE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            call(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = APPROVE_TX_INFO.copy(from = WalletAddress("dead"))

        suppose("transaction is returned") {
            call(blockchainService.fetchTransactionInfo(chainSpec, APPROVE_TX_HASH, APPROVAL_EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "approve",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.disperseContractAddress),
                        FunctionArgument(TOTAL_TOKEN_AMOUNT)
                    )
                )
            )
                .willReturn(ENCODED_APPROVE_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with failed approve status is returned") {
            expectThat(service.getAssetMultiSendRequest(ID))
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = Status.FAILED,
                        approveData = ENCODED_APPROVE_DATA,
                        approveTransactionInfo = transactionInfo,
                        disperseStatus = null,
                        disperseData = null,
                        disperseValue = null,
                        disperseTransactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithFailedDisperseStatusWhenDisperseTransactionHasMismatchingFromAddress() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(tokenAddress = null, disperseTxHash = DISPERSE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            call(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = DISPERSE_ETHER_TX_INFO.copy(from = WalletAddress("dead"))

        suppose("transaction is returned") {
            call(blockchainService.fetchTransactionInfo(chainSpec, DISPERSE_TX_HASH, TRANSFER_EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "disperseEther",
                    arguments = listOf(
                        FunctionArgument.fromAddresses(CREATE_PARAMS.assetRecipientAddresses),
                        FunctionArgument.fromUint256s(CREATE_PARAMS.assetAmounts)
                    )
                )
            )
                .willReturn(ENCODED_DISPERSE_ETHER_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with failed disperse status is returned") {
            expectThat(service.getAssetMultiSendRequest(ID))
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = null,
                        approveData = null,
                        approveTransactionInfo = null,
                        disperseStatus = Status.FAILED,
                        disperseData = ENCODED_DISPERSE_ETHER_DATA,
                        disperseValue = TOTAL_TOKEN_AMOUNT,
                        disperseTransactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithFailedApproveStatusWhenApproveTransactionHasMismatchingToAddress() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(approveTxHash = APPROVE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            call(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = APPROVE_TX_INFO.copy(to = WalletAddress("dead"))

        suppose("transaction is returned") {
            call(blockchainService.fetchTransactionInfo(chainSpec, APPROVE_TX_HASH, APPROVAL_EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "approve",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.disperseContractAddress),
                        FunctionArgument(TOTAL_TOKEN_AMOUNT)
                    )
                )
            )
                .willReturn(ENCODED_APPROVE_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with failed approve status is returned") {
            expectThat(service.getAssetMultiSendRequest(ID))
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = Status.FAILED,
                        approveData = ENCODED_APPROVE_DATA,
                        approveTransactionInfo = transactionInfo,
                        disperseStatus = null,
                        disperseData = null,
                        disperseValue = null,
                        disperseTransactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithFailedDisperseStatusWhenDisperseTransactionHasMismatchingToAddress() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(tokenAddress = null, disperseTxHash = DISPERSE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            call(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = DISPERSE_ETHER_TX_INFO.copy(to = WalletAddress("dead"))

        suppose("transaction is returned") {
            call(blockchainService.fetchTransactionInfo(chainSpec, DISPERSE_TX_HASH, TRANSFER_EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "disperseEther",
                    arguments = listOf(
                        FunctionArgument.fromAddresses(CREATE_PARAMS.assetRecipientAddresses),
                        FunctionArgument.fromUint256s(CREATE_PARAMS.assetAmounts)
                    )
                )
            )
                .willReturn(ENCODED_DISPERSE_ETHER_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with failed disperse status is returned") {
            expectThat(service.getAssetMultiSendRequest(ID))
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = null,
                        approveData = null,
                        approveTransactionInfo = null,
                        disperseStatus = Status.FAILED,
                        disperseData = ENCODED_DISPERSE_ETHER_DATA,
                        disperseValue = TOTAL_TOKEN_AMOUNT,
                        disperseTransactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithFailedApproveStatusWhenApproveTransactionHasNonNullDeployedCtrAddr() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(approveTxHash = APPROVE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            call(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = APPROVE_TX_INFO.copy(deployedContractAddress = ContractAddress("dead"))

        suppose("transaction is returned") {
            call(blockchainService.fetchTransactionInfo(chainSpec, APPROVE_TX_HASH, APPROVAL_EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "approve",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.disperseContractAddress),
                        FunctionArgument(TOTAL_TOKEN_AMOUNT)
                    )
                )
            )
                .willReturn(ENCODED_APPROVE_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with failed approve status is returned") {
            expectThat(service.getAssetMultiSendRequest(ID))
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = Status.FAILED,
                        approveData = ENCODED_APPROVE_DATA,
                        approveTransactionInfo = transactionInfo,
                        disperseStatus = null,
                        disperseData = null,
                        disperseValue = null,
                        disperseTransactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithFailedDisperseStatusWhenDisperseTransactionHasNonNullDeployedContractAddr() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(tokenAddress = null, disperseTxHash = DISPERSE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            call(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = DISPERSE_ETHER_TX_INFO.copy(deployedContractAddress = ContractAddress("dead"))

        suppose("transaction is returned") {
            call(blockchainService.fetchTransactionInfo(chainSpec, DISPERSE_TX_HASH, TRANSFER_EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "disperseEther",
                    arguments = listOf(
                        FunctionArgument.fromAddresses(CREATE_PARAMS.assetRecipientAddresses),
                        FunctionArgument.fromUint256s(CREATE_PARAMS.assetAmounts)
                    )
                )
            )
                .willReturn(ENCODED_DISPERSE_ETHER_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with failed disperse status is returned") {
            expectThat(service.getAssetMultiSendRequest(ID))
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = null,
                        approveData = null,
                        approveTransactionInfo = null,
                        disperseStatus = Status.FAILED,
                        disperseData = ENCODED_DISPERSE_ETHER_DATA,
                        disperseValue = TOTAL_TOKEN_AMOUNT,
                        disperseTransactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithFailedApproveStatusWhenApproveTransactionHasMismatchingData() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(approveTxHash = APPROVE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            call(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = APPROVE_TX_INFO.copy(data = FunctionData("mismatching"))

        suppose("transaction is returned") {
            call(blockchainService.fetchTransactionInfo(chainSpec, APPROVE_TX_HASH, APPROVAL_EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "approve",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.disperseContractAddress),
                        FunctionArgument(TOTAL_TOKEN_AMOUNT)
                    )
                )
            )
                .willReturn(ENCODED_APPROVE_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with failed approve status is returned") {
            expectThat(service.getAssetMultiSendRequest(ID))
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = Status.FAILED,
                        approveData = ENCODED_APPROVE_DATA,
                        approveTransactionInfo = transactionInfo,
                        disperseStatus = null,
                        disperseData = null,
                        disperseValue = null,
                        disperseTransactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithFailedDisperseStatusWhenDisperseTransactionHasMismatchingData() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(tokenAddress = null, disperseTxHash = DISPERSE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            call(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = DISPERSE_ETHER_TX_INFO.copy(data = FunctionData("mismatching"))

        suppose("transaction is returned") {
            call(blockchainService.fetchTransactionInfo(chainSpec, DISPERSE_TX_HASH, TRANSFER_EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "disperseEther",
                    arguments = listOf(
                        FunctionArgument.fromAddresses(CREATE_PARAMS.assetRecipientAddresses),
                        FunctionArgument.fromUint256s(CREATE_PARAMS.assetAmounts)
                    )
                )
            )
                .willReturn(ENCODED_DISPERSE_ETHER_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with failed disperse status is returned") {
            expectThat(service.getAssetMultiSendRequest(ID))
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = null,
                        approveData = null,
                        approveTransactionInfo = null,
                        disperseStatus = Status.FAILED,
                        disperseData = ENCODED_DISPERSE_ETHER_DATA,
                        disperseValue = TOTAL_TOKEN_AMOUNT,
                        disperseTransactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithFailedApproveStatusWhenApproveTransactionHasMismatchingValue() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(approveTxHash = APPROVE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            call(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = APPROVE_TX_INFO.copy(value = Balance(BigInteger.ONE))

        suppose("transaction is returned") {
            call(blockchainService.fetchTransactionInfo(chainSpec, APPROVE_TX_HASH, APPROVAL_EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "approve",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.disperseContractAddress),
                        FunctionArgument(TOTAL_TOKEN_AMOUNT)
                    )
                )
            )
                .willReturn(ENCODED_APPROVE_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with failed approve status is returned") {
            expectThat(service.getAssetMultiSendRequest(ID))
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = Status.FAILED,
                        approveData = ENCODED_APPROVE_DATA,
                        approveTransactionInfo = transactionInfo,
                        disperseStatus = null,
                        disperseData = null,
                        disperseValue = null,
                        disperseTransactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithFailedDisperseStatusWhenDisperseTransactionHasMismatchingValue() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(tokenAddress = null, disperseTxHash = DISPERSE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            call(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = DISPERSE_ETHER_TX_INFO.copy(value = Balance.ZERO)

        suppose("transaction is returned") {
            call(blockchainService.fetchTransactionInfo(chainSpec, DISPERSE_TX_HASH, TRANSFER_EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "disperseEther",
                    arguments = listOf(
                        FunctionArgument.fromAddresses(CREATE_PARAMS.assetRecipientAddresses),
                        FunctionArgument.fromUint256s(CREATE_PARAMS.assetAmounts)
                    )
                )
            )
                .willReturn(ENCODED_DISPERSE_ETHER_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with failed disperse status is returned") {
            expectThat(service.getAssetMultiSendRequest(ID))
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = null,
                        approveData = null,
                        approveTransactionInfo = null,
                        disperseStatus = Status.FAILED,
                        disperseData = ENCODED_DISPERSE_ETHER_DATA,
                        disperseValue = TOTAL_TOKEN_AMOUNT,
                        disperseTransactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithSuccessfulApproveStatusWhenSenderIsNotNull() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(approveTxHash = APPROVE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            call(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = APPROVE_TX_INFO

        suppose("transaction is returned") {
            call(blockchainService.fetchTransactionInfo(chainSpec, APPROVE_TX_HASH, APPROVAL_EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded for approve transaction") {
            call(
                functionEncoderService.encode(
                    functionName = "approve",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.disperseContractAddress),
                        FunctionArgument(TOTAL_TOKEN_AMOUNT)
                    )
                )
            )
                .willReturn(ENCODED_APPROVE_DATA)
        }

        suppose("function data will be encoded for disperse transaction") {
            call(
                functionEncoderService.encode(
                    functionName = "disperseToken",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.tokenAddress!!),
                        FunctionArgument.fromAddresses(CREATE_PARAMS.assetRecipientAddresses),
                        FunctionArgument.fromUint256s(CREATE_PARAMS.assetAmounts)
                    )
                )
            )
                .willReturn(ENCODED_DISPERSE_TOKEN_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with successful approve status is returned") {
            expectThat(service.getAssetMultiSendRequest(ID))
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = Status.SUCCESS,
                        approveData = ENCODED_APPROVE_DATA,
                        approveTransactionInfo = transactionInfo,
                        disperseStatus = Status.PENDING,
                        disperseData = ENCODED_DISPERSE_TOKEN_DATA,
                        disperseValue = Balance.ZERO,
                        disperseTransactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithSuccessfulDisperseStatusWhenSenderIsNotNull() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(tokenAddress = null, disperseTxHash = DISPERSE_TX_HASH)

        suppose("asset multi-send request exists in database") {
            call(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = DISPERSE_ETHER_TX_INFO

        suppose("transaction is returned") {
            call(blockchainService.fetchTransactionInfo(chainSpec, DISPERSE_TX_HASH, TRANSFER_EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "disperseEther",
                    arguments = listOf(
                        FunctionArgument.fromAddresses(CREATE_PARAMS.assetRecipientAddresses),
                        FunctionArgument.fromUint256s(CREATE_PARAMS.assetAmounts)
                    )
                )
            )
                .willReturn(ENCODED_DISPERSE_ETHER_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with successful disperse status is returned") {
            expectThat(service.getAssetMultiSendRequest(ID))
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = null,
                        approveData = null,
                        approveTransactionInfo = null,
                        disperseStatus = Status.SUCCESS,
                        disperseData = ENCODED_DISPERSE_ETHER_DATA,
                        disperseValue = TOTAL_TOKEN_AMOUNT,
                        disperseTransactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithSuccessfulApproveStatusWhenSenderIsNull() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(approveTxHash = APPROVE_TX_HASH, assetSenderAddress = null)

        suppose("asset multi-send request exists in database") {
            call(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = APPROVE_TX_INFO

        suppose("transaction is returned") {
            call(blockchainService.fetchTransactionInfo(chainSpec, APPROVE_TX_HASH, APPROVAL_EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded for approve transaction") {
            call(
                functionEncoderService.encode(
                    functionName = "approve",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.disperseContractAddress),
                        FunctionArgument(TOTAL_TOKEN_AMOUNT)
                    )
                )
            )
                .willReturn(ENCODED_APPROVE_DATA)
        }

        suppose("function data will be encoded for disperse transaction") {
            call(
                functionEncoderService.encode(
                    functionName = "disperseToken",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.tokenAddress!!),
                        FunctionArgument.fromAddresses(CREATE_PARAMS.assetRecipientAddresses),
                        FunctionArgument.fromUint256s(CREATE_PARAMS.assetAmounts)
                    )
                )
            )
                .willReturn(ENCODED_DISPERSE_TOKEN_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with pending successful status is returned") {
            expectThat(service.getAssetMultiSendRequest(ID))
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = Status.SUCCESS,
                        approveData = ENCODED_APPROVE_DATA,
                        approveTransactionInfo = transactionInfo,
                        disperseStatus = Status.PENDING,
                        disperseData = ENCODED_DISPERSE_TOKEN_DATA,
                        disperseValue = Balance.ZERO,
                        disperseTransactionInfo = null
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithSuccessfulDisperseStatusWhenSenderIsNull() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(
            tokenAddress = null,
            disperseTxHash = DISPERSE_TX_HASH,
            assetSenderAddress = null
        )

        suppose("asset multi-send request exists in database") {
            call(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)
        val transactionInfo = DISPERSE_ETHER_TX_INFO

        suppose("transaction is returned") {
            call(blockchainService.fetchTransactionInfo(chainSpec, DISPERSE_TX_HASH, TRANSFER_EVENTS))
                .willReturn(transactionInfo)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "disperseEther",
                    arguments = listOf(
                        FunctionArgument.fromAddresses(CREATE_PARAMS.assetRecipientAddresses),
                        FunctionArgument.fromUint256s(CREATE_PARAMS.assetAmounts)
                    )
                )
            )
                .willReturn(ENCODED_DISPERSE_ETHER_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with successful disperse status is returned") {
            expectThat(service.getAssetMultiSendRequest(ID))
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = null,
                        approveData = null,
                        approveTransactionInfo = null,
                        disperseStatus = Status.SUCCESS,
                        disperseData = ENCODED_DISPERSE_ETHER_DATA,
                        disperseValue = TOTAL_TOKEN_AMOUNT,
                        disperseTransactionInfo = transactionInfo
                    )
                )
        }
    }

    @Test
    fun mustReturnAssetMultiSendRequestWithSuccessfulApproveStatusAndDisperseStatus() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val storedRequest = STORED_REQUEST.copy(
            approveTxHash = APPROVE_TX_HASH,
            disperseTxHash = DISPERSE_TX_HASH
        )

        suppose("asset multi-send request exists in database") {
            call(assetMultiSendRequestRepository.getById(ID))
                .willReturn(storedRequest)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(storedRequest.chainId, null)

        suppose("approve transaction is returned") {
            call(blockchainService.fetchTransactionInfo(chainSpec, APPROVE_TX_HASH, APPROVAL_EVENTS))
                .willReturn(APPROVE_TX_INFO)
        }

        suppose("disperse transaction is returned") {
            call(blockchainService.fetchTransactionInfo(chainSpec, DISPERSE_TX_HASH, TRANSFER_EVENTS))
                .willReturn(DISPERSE_TOKEN_TX_INFO)
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded for approve transaction") {
            call(
                functionEncoderService.encode(
                    functionName = "approve",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.disperseContractAddress),
                        FunctionArgument(TOTAL_TOKEN_AMOUNT)
                    )
                )
            )
                .willReturn(ENCODED_APPROVE_DATA)
        }

        suppose("function data will be encoded for disperse transaction") {
            call(
                functionEncoderService.encode(
                    functionName = "disperseToken",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.tokenAddress!!),
                        FunctionArgument.fromAddresses(CREATE_PARAMS.assetRecipientAddresses),
                        FunctionArgument.fromUint256s(CREATE_PARAMS.assetAmounts)
                    )
                )
            )
                .willReturn(ENCODED_DISPERSE_TOKEN_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = blockchainService
            ),
            projectRepository = projectRepositoryMock(storedRequest.projectId)
        )

        verify("asset multi-send request with successful approve status is returned") {
            expectThat(service.getAssetMultiSendRequest(ID))
                .isEqualTo(
                    storedRequest.withMultiTransactionData(
                        approveStatus = Status.SUCCESS,
                        approveData = ENCODED_APPROVE_DATA,
                        approveTransactionInfo = APPROVE_TX_INFO,
                        disperseStatus = Status.SUCCESS,
                        disperseData = ENCODED_DISPERSE_TOKEN_DATA,
                        disperseValue = Balance.ZERO,
                        disperseTransactionInfo = DISPERSE_TOKEN_TX_INFO
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyReturnListOfAssetMultiSendRequestsByProjectId() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()

        suppose("asset multi-send request exists in database") {
            call(assetMultiSendRequestRepository.getAllByProjectId(PROJECT.id))
                .willReturn(listOf(STORED_REQUEST))
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "approve",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.disperseContractAddress),
                        FunctionArgument(TOTAL_TOKEN_AMOUNT)
                    )
                )
            )
                .willReturn(ENCODED_APPROVE_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(STORED_REQUEST.projectId)
        )

        verify("asset multi-send request is returned") {
            expectThat(service.getAssetMultiSendRequestsByProjectId(PROJECT.id))
                .isEqualTo(
                    listOf(
                        STORED_REQUEST.withMultiTransactionData(
                            approveStatus = Status.PENDING,
                            approveData = ENCODED_APPROVE_DATA,
                            approveTransactionInfo = null,
                            disperseStatus = null,
                            disperseData = null,
                            disperseValue = null,
                            disperseTransactionInfo = null
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyReturnEmptyListOfAssetMultiSendRequestsForNonExistentProject() {
        val projectId = ProjectId(UUID.randomUUID())
        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = mock(),
            assetMultiSendRequestRepository = mock(),
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(projectId)
        )

        verify("empty list is returned") {
            val result = service.getAssetMultiSendRequestsByProjectId(projectId)

            expectThat(result)
                .isEmpty()
        }
    }

    @Test
    fun mustCorrectlyReturnListOfAssetMultiSendRequestsBySenderAddress() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()

        suppose("asset multi-send request exists in database") {
            call(assetMultiSendRequestRepository.getBySender(STORED_REQUEST.assetSenderAddress!!))
                .willReturn(listOf(STORED_REQUEST))
        }

        val functionEncoderService = mock<FunctionEncoderService>()

        suppose("function data will be encoded") {
            call(
                functionEncoderService.encode(
                    functionName = "approve",
                    arguments = listOf(
                        FunctionArgument(CREATE_PARAMS.disperseContractAddress),
                        FunctionArgument(TOTAL_TOKEN_AMOUNT)
                    )
                )
            )
                .willReturn(ENCODED_APPROVE_DATA)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = functionEncoderService,
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = projectRepositoryMock(STORED_REQUEST.projectId)
        )

        verify("asset multi-send request with successful status is returned") {
            expectThat(service.getAssetMultiSendRequestsBySender(STORED_REQUEST.assetSenderAddress!!))
                .isEqualTo(
                    listOf(
                        STORED_REQUEST.withMultiTransactionData(
                            approveStatus = Status.PENDING,
                            approveData = ENCODED_APPROVE_DATA,
                            approveTransactionInfo = null,
                            disperseStatus = null,
                            disperseData = null,
                            disperseValue = null,
                            disperseTransactionInfo = null
                        )
                    )
                )
        }
    }

    @Test
    fun mustSuccessfullyAttachApproveTxInfo() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val id = AssetMultiSendRequestId(UUID.randomUUID())
        val caller = WalletAddress("0xbc25524e0daacB1F149BA55279f593F5E3FB73e9")

        suppose("approve txInfo will be successfully attached to the request") {
            call(assetMultiSendRequestRepository.setApproveTxInfo(id, APPROVE_TX_HASH, caller))
                .willReturn(true)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = mock(),
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("approve txInfo was successfully attached") {
            service.attachApproveTxInfo(id, APPROVE_TX_HASH, caller)

            expectInteractions(assetMultiSendRequestRepository) {
                once.setApproveTxInfo(id, APPROVE_TX_HASH, caller)
            }
        }
    }

    @Test
    fun mustThrowCannotAttachTxInfoExceptionWhenAttachingApproveTxInfoFails() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val id = AssetMultiSendRequestId(UUID.randomUUID())
        val caller = WalletAddress("0xbc25524e0daacB1F149BA55279f593F5E3FB73e9")

        suppose("attaching approve txInfo will fail") {
            call(assetMultiSendRequestRepository.setApproveTxInfo(id, APPROVE_TX_HASH, caller))
                .willReturn(false)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = mock(),
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("CannotAttachTxInfoException is thrown") {
            expectThrows<CannotAttachTxInfoException> {
                service.attachApproveTxInfo(id, APPROVE_TX_HASH, caller)
            }

            expectInteractions(assetMultiSendRequestRepository) {
                once.setApproveTxInfo(id, APPROVE_TX_HASH, caller)
            }
        }
    }

    @Test
    fun mustSuccessfullyDisperseApproveTxInfo() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val id = AssetMultiSendRequestId(UUID.randomUUID())
        val caller = WalletAddress("0xbc25524e0daacB1F149BA55279f593F5E3FB73e9")

        suppose("disperse txInfo will be successfully attached to the request") {
            call(assetMultiSendRequestRepository.setDisperseTxInfo(id, DISPERSE_TX_HASH, caller))
                .willReturn(true)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = mock(),
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("disperse txInfo was successfully attached") {
            service.attachDisperseTxInfo(id, DISPERSE_TX_HASH, caller)

            expectInteractions(assetMultiSendRequestRepository) {
                once.setDisperseTxInfo(id, DISPERSE_TX_HASH, caller)
            }
        }
    }

    @Test
    fun mustThrowCannotAttachTxInfoExceptionWhenAttachingDisperseTxInfoFails() {
        val assetMultiSendRequestRepository = mock<AssetMultiSendRequestRepository>()
        val id = AssetMultiSendRequestId(UUID.randomUUID())
        val caller = WalletAddress("0xbc25524e0daacB1F149BA55279f593F5E3FB73e9")

        suppose("attaching disperse txInfo will fail") {
            call(assetMultiSendRequestRepository.setDisperseTxInfo(id, APPROVE_TX_HASH, caller))
                .willReturn(false)
        }

        val service = AssetMultiSendRequestServiceImpl(
            functionEncoderService = mock(),
            assetMultiSendRequestRepository = assetMultiSendRequestRepository,
            ethCommonService = EthCommonServiceImpl(
                uuidProvider = mock(),
                utcDateTimeProvider = mock(),
                blockchainService = mock()
            ),
            projectRepository = mock()
        )

        verify("CannotAttachTxInfoException is thrown") {
            expectThrows<CannotAttachTxInfoException> {
                service.attachDisperseTxInfo(id, APPROVE_TX_HASH, caller)
            }

            expectInteractions(assetMultiSendRequestRepository) {
                once.setDisperseTxInfo(id, APPROVE_TX_HASH, caller)
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
