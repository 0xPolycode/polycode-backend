package polycode.controller

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import polycode.JsonSchemaDocumentation
import polycode.TestBase
import polycode.TestData
import polycode.features.api.access.model.result.Project
import polycode.features.asset.multisend.controller.AssetMultiSendRequestController
import polycode.features.asset.multisend.model.params.CreateAssetMultiSendRequestParams
import polycode.features.asset.multisend.model.request.CreateAssetMultiSendRequest
import polycode.features.asset.multisend.model.request.MultiPaymentTemplateItemRequest
import polycode.features.asset.multisend.model.response.AssetMultiSendRequestResponse
import polycode.features.asset.multisend.model.response.AssetMultiSendRequestsResponse
import polycode.features.asset.multisend.model.response.MultiSendItemResponse
import polycode.features.asset.multisend.model.result.AssetMultiSendRequest
import polycode.features.asset.multisend.service.AssetMultiSendRequestService
import polycode.generated.jooq.id.AssetMultiSendRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.model.ScreenConfig
import polycode.model.request.AttachTransactionInfoRequest
import polycode.model.response.TransactionResponse
import polycode.util.AssetType
import polycode.util.Balance
import polycode.util.BaseUrl
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.FunctionData
import polycode.util.Status
import polycode.util.TransactionData
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import polycode.util.WithFunctionDataOrEthValue
import polycode.util.WithMultiTransactionData
import java.math.BigInteger
import java.util.UUID

class AssetMultiSendRequestControllerTest : TestBase() {

    @Test
    fun mustCorrectlyCreateAssetMultiSendRequest() {
        val params = CreateAssetMultiSendRequestParams(
            redirectUrl = "redirect-url",
            tokenAddress = ContractAddress("a"),
            disperseContractAddress = ContractAddress("b"),
            assetAmounts = listOf(Balance(BigInteger.TEN)),
            assetRecipientAddresses = listOf(WalletAddress("c")),
            itemNames = listOf("name"),
            assetSenderAddress = WalletAddress("b"),
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
        val result = AssetMultiSendRequest(
            id = AssetMultiSendRequestId(UUID.randomUUID()),
            projectId = ProjectId(UUID.randomUUID()),
            chainId = ChainId(1337L),
            redirectUrl = params.redirectUrl!!,
            tokenAddress = params.tokenAddress,
            disperseContractAddress = params.disperseContractAddress,
            assetAmounts = params.assetAmounts,
            assetRecipientAddresses = params.assetRecipientAddresses,
            itemNames = params.itemNames,
            assetSenderAddress = params.assetSenderAddress,
            approveTxHash = null,
            disperseTxHash = null,
            arbitraryData = params.arbitraryData,
            approveScreenConfig = params.approveScreenConfig,
            disperseScreenConfig = params.disperseScreenConfig,
            createdAt = TestData.TIMESTAMP
        )
        val project = Project(
            id = result.projectId,
            ownerId = UserId(UUID.randomUUID()),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = ChainId(1337L),
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        val data = FunctionData("approve-data")
        val service = mock<AssetMultiSendRequestService>()

        suppose("asset multi-send request will be created") {
            call(service.createAssetMultiSendRequest(params, project))
                .willReturn(WithFunctionDataOrEthValue(result, data, null))
        }

        val controller = AssetMultiSendRequestController(service)

        verify("controller returns correct response") {
            val request = CreateAssetMultiSendRequest(
                redirectUrl = params.redirectUrl,
                tokenAddress = params.tokenAddress?.rawValue,
                disperseContractAddress = params.disperseContractAddress.rawValue,
                assetType = AssetType.TOKEN,
                items = listOf(
                    MultiPaymentTemplateItemRequest(
                        walletAddress = params.assetRecipientAddresses[0].rawValue,
                        amount = params.assetAmounts[0].rawValue,
                        itemName = params.itemNames[0]
                    )
                ),
                senderAddress = params.assetSenderAddress?.rawValue,
                arbitraryData = params.arbitraryData,
                approveScreenConfig = params.approveScreenConfig,
                disperseScreenConfig = params.disperseScreenConfig
            )
            val response = controller.createAssetMultiSendRequest(project, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        AssetMultiSendRequestResponse(
                            id = result.id,
                            projectId = project.id,
                            approveStatus = Status.PENDING,
                            disperseStatus = null,
                            chainId = result.chainId.value,
                            tokenAddress = result.tokenAddress?.rawValue,
                            disperseContractAddress = result.disperseContractAddress.rawValue,
                            assetType = AssetType.TOKEN,
                            items = listOf(
                                MultiSendItemResponse(
                                    walletAddress = result.assetRecipientAddresses[0].rawValue,
                                    amount = result.assetAmounts[0].rawValue,
                                    itemName = result.itemNames[0]
                                )
                            ),
                            senderAddress = result.assetSenderAddress?.rawValue,
                            arbitraryData = result.arbitraryData,
                            approveScreenConfig = result.approveScreenConfig,
                            disperseScreenConfig = result.disperseScreenConfig,
                            redirectUrl = result.redirectUrl,
                            approveTx = TransactionResponse(
                                txHash = null,
                                from = result.assetSenderAddress?.rawValue,
                                to = result.tokenAddress!!.rawValue,
                                data = data.value,
                                value = BigInteger.ZERO,
                                blockConfirmations = null,
                                timestamp = null
                            ),
                            disperseTx = null,
                            createdAt = TestData.TIMESTAMP.value,
                            approveEvents = null,
                            disperseEvents = null
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchAssetMultiSendRequest() {
        val id = AssetMultiSendRequestId(UUID.randomUUID())
        val service = mock<AssetMultiSendRequestService>()
        val approveTxHash = TransactionHash("approve-tx-hash")
        val disperseTxHash = TransactionHash("disperse-tx-hash")
        val result = WithMultiTransactionData(
            value = AssetMultiSendRequest(
                id = id,
                projectId = ProjectId(UUID.randomUUID()),
                chainId = ChainId(123L),
                redirectUrl = "redirect-url",
                tokenAddress = ContractAddress("a"),
                disperseContractAddress = ContractAddress("b"),
                assetAmounts = listOf(Balance(BigInteger.TEN)),
                assetRecipientAddresses = listOf(WalletAddress("c")),
                itemNames = listOf("name"),
                assetSenderAddress = WalletAddress("d"),
                approveTxHash = approveTxHash,
                disperseTxHash = disperseTxHash,
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                approveScreenConfig = ScreenConfig(
                    beforeActionMessage = "approve-before-action-message",
                    afterActionMessage = "approve-after-action-message"
                ),
                disperseScreenConfig = ScreenConfig(
                    beforeActionMessage = "disperse-before-action-message",
                    afterActionMessage = "disperse-after-action-message"
                ),
                createdAt = TestData.TIMESTAMP
            ),
            approveStatus = Status.SUCCESS,
            approveTransactionData = TransactionData(
                txHash = approveTxHash,
                fromAddress = WalletAddress("d"),
                toAddress = ContractAddress("a"),
                data = FunctionData("approve-data"),
                value = Balance.ZERO,
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP,
                events = emptyList()
            ),
            disperseStatus = Status.SUCCESS,
            disperseTransactionData = TransactionData(
                txHash = disperseTxHash,
                fromAddress = WalletAddress("d"),
                toAddress = ContractAddress("b"),
                data = FunctionData("disperse-data"),
                value = Balance(BigInteger.TEN),
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP,
                events = emptyList()
            )
        )

        suppose("some asset multi-send request will be fetched") {
            call(service.getAssetMultiSendRequest(id))
                .willReturn(result)
        }

        val controller = AssetMultiSendRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getAssetMultiSendRequest(id)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        AssetMultiSendRequestResponse(
                            id = result.value.id,
                            projectId = result.value.projectId,
                            approveStatus = Status.SUCCESS,
                            disperseStatus = Status.SUCCESS,
                            chainId = result.value.chainId.value,
                            tokenAddress = result.value.tokenAddress?.rawValue,
                            disperseContractAddress = result.value.disperseContractAddress.rawValue,
                            assetType = AssetType.TOKEN,
                            items = listOf(
                                MultiSendItemResponse(
                                    walletAddress = result.value.assetRecipientAddresses[0].rawValue,
                                    amount = result.value.assetAmounts[0].rawValue,
                                    itemName = result.value.itemNames[0]
                                )
                            ),
                            senderAddress = result.value.assetSenderAddress?.rawValue,
                            arbitraryData = result.value.arbitraryData,
                            approveScreenConfig = result.value.approveScreenConfig,
                            disperseScreenConfig = result.value.disperseScreenConfig,
                            redirectUrl = result.value.redirectUrl,
                            approveTx = TransactionResponse(
                                txHash = approveTxHash.value,
                                from = result.value.assetSenderAddress?.rawValue,
                                to = result.value.tokenAddress!!.rawValue,
                                data = result.approveTransactionData?.data?.value!!,
                                value = BigInteger.ZERO,
                                blockConfirmations = result.approveTransactionData?.blockConfirmations!!,
                                timestamp = result.approveTransactionData?.timestamp?.value!!
                            ),
                            disperseTx = TransactionResponse(
                                txHash = disperseTxHash.value,
                                from = result.value.assetSenderAddress?.rawValue,
                                to = result.value.disperseContractAddress.rawValue,
                                data = result.disperseTransactionData?.data?.value!!,
                                value = BigInteger.TEN,
                                blockConfirmations = result.disperseTransactionData?.blockConfirmations!!,
                                timestamp = result.disperseTransactionData?.timestamp?.value!!
                            ),
                            createdAt = TestData.TIMESTAMP.value,
                            approveEvents = emptyList(),
                            disperseEvents = emptyList()
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchAssetMultiSendRequestsByProjectId() {
        val id = AssetMultiSendRequestId(UUID.randomUUID())
        val projectId = ProjectId(UUID.randomUUID())
        val service = mock<AssetMultiSendRequestService>()
        val approveTxHash = TransactionHash("approve-tx-hash")
        val disperseTxHash = TransactionHash("disperse-tx-hash")
        val result = WithMultiTransactionData(
            value = AssetMultiSendRequest(
                id = id,
                projectId = projectId,
                chainId = ChainId(123L),
                redirectUrl = "redirect-url",
                tokenAddress = ContractAddress("a"),
                disperseContractAddress = ContractAddress("b"),
                assetAmounts = listOf(Balance(BigInteger.TEN)),
                assetRecipientAddresses = listOf(WalletAddress("c")),
                itemNames = listOf("name"),
                assetSenderAddress = WalletAddress("d"),
                approveTxHash = approveTxHash,
                disperseTxHash = disperseTxHash,
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                approveScreenConfig = ScreenConfig(
                    beforeActionMessage = "approve-before-action-message",
                    afterActionMessage = "approve-after-action-message"
                ),
                disperseScreenConfig = ScreenConfig(
                    beforeActionMessage = "disperse-before-action-message",
                    afterActionMessage = "disperse-after-action-message"
                ),
                createdAt = TestData.TIMESTAMP
            ),
            approveStatus = Status.SUCCESS,
            approveTransactionData = TransactionData(
                txHash = approveTxHash,
                fromAddress = WalletAddress("d"),
                toAddress = ContractAddress("a"),
                data = FunctionData("approve-data"),
                value = Balance.ZERO,
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP,
                events = emptyList()
            ),
            disperseStatus = Status.SUCCESS,
            disperseTransactionData = TransactionData(
                txHash = disperseTxHash,
                fromAddress = WalletAddress("d"),
                toAddress = ContractAddress("b"),
                data = FunctionData("disperse-data"),
                value = Balance(BigInteger.TEN),
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP,
                events = emptyList()
            )
        )

        suppose("some asset multi-send request will be fetched") {
            call(service.getAssetMultiSendRequestsByProjectId(projectId))
                .willReturn(listOf(result))
        }

        val controller = AssetMultiSendRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getAssetMultiSendRequestsByProjectId(projectId)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        AssetMultiSendRequestsResponse(
                            listOf(
                                AssetMultiSendRequestResponse(
                                    id = result.value.id,
                                    projectId = result.value.projectId,
                                    approveStatus = Status.SUCCESS,
                                    disperseStatus = Status.SUCCESS,
                                    chainId = result.value.chainId.value,
                                    tokenAddress = result.value.tokenAddress?.rawValue,
                                    disperseContractAddress = result.value.disperseContractAddress.rawValue,
                                    assetType = AssetType.TOKEN,
                                    items = listOf(
                                        MultiSendItemResponse(
                                            walletAddress = result.value.assetRecipientAddresses[0].rawValue,
                                            amount = result.value.assetAmounts[0].rawValue,
                                            itemName = result.value.itemNames[0]
                                        )
                                    ),
                                    senderAddress = result.value.assetSenderAddress?.rawValue,
                                    arbitraryData = result.value.arbitraryData,
                                    approveScreenConfig = result.value.approveScreenConfig,
                                    disperseScreenConfig = result.value.disperseScreenConfig,
                                    redirectUrl = result.value.redirectUrl,
                                    approveTx = TransactionResponse(
                                        txHash = approveTxHash.value,
                                        from = result.value.assetSenderAddress?.rawValue,
                                        to = result.value.tokenAddress!!.rawValue,
                                        data = result.approveTransactionData?.data?.value!!,
                                        value = BigInteger.ZERO,
                                        blockConfirmations = result.approveTransactionData?.blockConfirmations!!,
                                        timestamp = result.approveTransactionData?.timestamp?.value!!
                                    ),
                                    disperseTx = TransactionResponse(
                                        txHash = disperseTxHash.value,
                                        from = result.value.assetSenderAddress?.rawValue,
                                        to = result.value.disperseContractAddress.rawValue,
                                        data = result.disperseTransactionData?.data?.value!!,
                                        value = BigInteger.TEN,
                                        blockConfirmations = result.disperseTransactionData?.blockConfirmations!!,
                                        timestamp = result.disperseTransactionData?.timestamp?.value!!
                                    ),
                                    createdAt = TestData.TIMESTAMP.value,
                                    approveEvents = emptyList(),
                                    disperseEvents = emptyList()
                                )
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchAssetMultiSendRequestsBySender() {
        val id = AssetMultiSendRequestId(UUID.randomUUID())
        val sender = WalletAddress("d")
        val service = mock<AssetMultiSendRequestService>()
        val approveTxHash = TransactionHash("approve-tx-hash")
        val disperseTxHash = TransactionHash("disperse-tx-hash")
        val result = WithMultiTransactionData(
            value = AssetMultiSendRequest(
                id = id,
                projectId = ProjectId(UUID.randomUUID()),
                chainId = ChainId(123L),
                redirectUrl = "redirect-url",
                tokenAddress = ContractAddress("a"),
                disperseContractAddress = ContractAddress("b"),
                assetAmounts = listOf(Balance(BigInteger.TEN)),
                assetRecipientAddresses = listOf(WalletAddress("c")),
                itemNames = listOf("name"),
                assetSenderAddress = sender,
                approveTxHash = approveTxHash,
                disperseTxHash = disperseTxHash,
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                approveScreenConfig = ScreenConfig(
                    beforeActionMessage = "approve-before-action-message",
                    afterActionMessage = "approve-after-action-message"
                ),
                disperseScreenConfig = ScreenConfig(
                    beforeActionMessage = "disperse-before-action-message",
                    afterActionMessage = "disperse-after-action-message"
                ),
                createdAt = TestData.TIMESTAMP
            ),
            approveStatus = Status.SUCCESS,
            approveTransactionData = TransactionData(
                txHash = approveTxHash,
                fromAddress = WalletAddress("d"),
                toAddress = ContractAddress("a"),
                data = FunctionData("approve-data"),
                value = Balance.ZERO,
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP,
                events = emptyList()
            ),
            disperseStatus = Status.SUCCESS,
            disperseTransactionData = TransactionData(
                txHash = disperseTxHash,
                fromAddress = WalletAddress("d"),
                toAddress = ContractAddress("b"),
                data = FunctionData("disperse-data"),
                value = Balance(BigInteger.TEN),
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP,
                events = emptyList()
            )
        )

        suppose("some asset multi-send request will be fetched") {
            call(service.getAssetMultiSendRequestsBySender(sender))
                .willReturn(listOf(result))
        }

        val controller = AssetMultiSendRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getAssetMultiSendRequestsBySender(sender.rawValue)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        AssetMultiSendRequestsResponse(
                            listOf(
                                AssetMultiSendRequestResponse(
                                    id = result.value.id,
                                    projectId = result.value.projectId,
                                    approveStatus = Status.SUCCESS,
                                    disperseStatus = Status.SUCCESS,
                                    chainId = result.value.chainId.value,
                                    tokenAddress = result.value.tokenAddress?.rawValue,
                                    disperseContractAddress = result.value.disperseContractAddress.rawValue,
                                    assetType = AssetType.TOKEN,
                                    items = listOf(
                                        MultiSendItemResponse(
                                            walletAddress = result.value.assetRecipientAddresses[0].rawValue,
                                            amount = result.value.assetAmounts[0].rawValue,
                                            itemName = result.value.itemNames[0]
                                        )
                                    ),
                                    senderAddress = result.value.assetSenderAddress?.rawValue,
                                    arbitraryData = result.value.arbitraryData,
                                    approveScreenConfig = result.value.approveScreenConfig,
                                    disperseScreenConfig = result.value.disperseScreenConfig,
                                    redirectUrl = result.value.redirectUrl,
                                    approveTx = TransactionResponse(
                                        txHash = approveTxHash.value,
                                        from = result.value.assetSenderAddress?.rawValue,
                                        to = result.value.tokenAddress!!.rawValue,
                                        data = result.approveTransactionData?.data?.value!!,
                                        value = BigInteger.ZERO,
                                        blockConfirmations = result.approveTransactionData?.blockConfirmations!!,
                                        timestamp = result.approveTransactionData?.timestamp?.value!!
                                    ),
                                    disperseTx = TransactionResponse(
                                        txHash = disperseTxHash.value,
                                        from = result.value.assetSenderAddress?.rawValue,
                                        to = result.value.disperseContractAddress.rawValue,
                                        data = result.disperseTransactionData?.data?.value!!,
                                        value = BigInteger.TEN,
                                        blockConfirmations = result.disperseTransactionData?.blockConfirmations!!,
                                        timestamp = result.disperseTransactionData?.timestamp?.value!!
                                    ),
                                    createdAt = TestData.TIMESTAMP.value,
                                    approveEvents = emptyList(),
                                    disperseEvents = emptyList()
                                )
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyAttachApproveTransactionInfo() {
        val service = mock<AssetMultiSendRequestService>()
        val controller = AssetMultiSendRequestController(service)

        val id = AssetMultiSendRequestId(UUID.randomUUID())
        val txHash = "approve-tx-hash"
        val caller = "c"

        suppose("approve transaction info will be attached") {
            val request = AttachTransactionInfoRequest(txHash, caller)
            controller.attachApproveTransactionInfo(id, request)
            JsonSchemaDocumentation.createSchema(request.javaClass)
        }

        verify("transaction info is correctly attached") {
            expectInteractions(service) {
                once.attachApproveTxInfo(id, TransactionHash(txHash), WalletAddress(caller))
            }
        }
    }

    @Test
    fun mustCorrectlyAttachDisperseTransactionInfo() {
        val service = mock<AssetMultiSendRequestService>()
        val controller = AssetMultiSendRequestController(service)

        val id = AssetMultiSendRequestId(UUID.randomUUID())
        val txHash = "disperse-tx-hash"
        val caller = "c"

        suppose("disperse transaction info will be attached") {
            val request = AttachTransactionInfoRequest(txHash, caller)
            controller.attachDisperseTransactionInfo(id, request)
            JsonSchemaDocumentation.createSchema(request.javaClass)
        }

        verify("transaction info is correctly attached") {
            expectInteractions(service) {
                once.attachDisperseTxInfo(id, TransactionHash(txHash), WalletAddress(caller))
            }
        }
    }
}
