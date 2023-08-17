package polycode.controller

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import polycode.JsonSchemaDocumentation
import polycode.TestBase
import polycode.TestData
import polycode.features.api.access.model.result.Project
import polycode.features.asset.balance.controller.AssetBalanceRequestController
import polycode.features.asset.balance.model.params.CreateAssetBalanceRequestParams
import polycode.features.asset.balance.model.request.CreateAssetBalanceRequest
import polycode.features.asset.balance.model.response.AssetBalanceRequestResponse
import polycode.features.asset.balance.model.response.AssetBalanceRequestsResponse
import polycode.features.asset.balance.model.response.BalanceResponse
import polycode.features.asset.balance.model.result.AssetBalanceRequest
import polycode.features.asset.balance.model.result.FullAssetBalanceRequest
import polycode.features.asset.balance.service.AssetBalanceRequestService
import polycode.generated.jooq.id.AssetBalanceRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.model.ScreenConfig
import polycode.model.request.AttachSignedMessageRequest
import polycode.util.AccountBalance
import polycode.util.AssetType
import polycode.util.Balance
import polycode.util.BaseUrl
import polycode.util.BlockNumber
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.SignedMessage
import polycode.util.Status
import polycode.util.UtcDateTime
import polycode.util.WalletAddress
import java.math.BigInteger
import java.util.UUID

class AssetBalanceRequestControllerTest : TestBase() {

    @Test
    fun mustCorrectlyCreateAssetBalanceRequest() {
        val params = CreateAssetBalanceRequestParams(
            redirectUrl = "redirect-url",
            tokenAddress = ContractAddress("a"),
            blockNumber = BlockNumber(BigInteger.TEN),
            requestedWalletAddress = WalletAddress("b"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        val result = AssetBalanceRequest(
            id = AssetBalanceRequestId(UUID.randomUUID()),
            projectId = ProjectId(UUID.randomUUID()),
            chainId = ChainId(1337L),
            redirectUrl = params.redirectUrl!!,
            tokenAddress = params.tokenAddress,
            blockNumber = params.blockNumber,
            requestedWalletAddress = params.requestedWalletAddress,
            actualWalletAddress = null,
            signedMessage = null,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = params.screenConfig,
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
        val service = mock<AssetBalanceRequestService>()

        suppose("asset balance request will be created") {
            call(service.createAssetBalanceRequest(params, project))
                .willReturn(result)
        }

        val controller = AssetBalanceRequestController(service)

        verify("controller returns correct response") {
            val request = CreateAssetBalanceRequest(
                redirectUrl = params.redirectUrl,
                tokenAddress = params.tokenAddress?.rawValue,
                assetType = AssetType.TOKEN,
                blockNumber = params.blockNumber?.value,
                walletAddress = params.requestedWalletAddress?.rawValue,
                arbitraryData = params.arbitraryData,
                screenConfig = params.screenConfig
            )
            val response = controller.createAssetBalanceRequest(project, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        AssetBalanceRequestResponse(
                            id = result.id,
                            projectId = project.id,
                            status = Status.PENDING,
                            chainId = result.chainId.value,
                            redirectUrl = result.redirectUrl,
                            tokenAddress = result.tokenAddress?.rawValue,
                            assetType = AssetType.TOKEN,
                            blockNumber = result.blockNumber?.value,
                            walletAddress = result.requestedWalletAddress?.rawValue,
                            arbitraryData = result.arbitraryData,
                            screenConfig = result.screenConfig.orEmpty(),
                            balance = null,
                            messageToSign = result.messageToSign,
                            signedMessage = result.signedMessage?.value,
                            createdAt = TestData.TIMESTAMP.value
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchAssetBalanceRequest() {
        val id = AssetBalanceRequestId(UUID.randomUUID())
        val service = mock<AssetBalanceRequestService>()
        val result = FullAssetBalanceRequest(
            id = id,
            projectId = ProjectId(UUID.randomUUID()),
            status = Status.SUCCESS,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "redirect-url",
            tokenAddress = ContractAddress("abc"),
            blockNumber = BlockNumber(BigInteger.TEN),
            requestedWalletAddress = WalletAddress("def"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            balance = AccountBalance(
                wallet = WalletAddress("def"),
                blockNumber = BlockNumber(BigInteger.TEN),
                timestamp = UtcDateTime.ofEpochSeconds(0L),
                amount = Balance(BigInteger.ONE)
            ),
            messageToSign = "message-to-sign",
            signedMessage = SignedMessage("signed-message"),
            createdAt = TestData.TIMESTAMP
        )

        suppose("some asset balance request will be fetched") {
            call(service.getAssetBalanceRequest(id))
                .willReturn(result)
        }

        val controller = AssetBalanceRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getAssetBalanceRequest(id)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        AssetBalanceRequestResponse(
                            id = result.id,
                            projectId = result.projectId,
                            status = result.status,
                            chainId = result.chainId.value,
                            redirectUrl = result.redirectUrl,
                            tokenAddress = result.tokenAddress?.rawValue,
                            assetType = AssetType.TOKEN,
                            blockNumber = result.blockNumber?.value,
                            walletAddress = result.requestedWalletAddress?.rawValue,
                            arbitraryData = result.arbitraryData,
                            screenConfig = result.screenConfig,
                            balance = result.balance?.let {
                                BalanceResponse(
                                    wallet = it.wallet.rawValue,
                                    blockNumber = it.blockNumber.value,
                                    timestamp = it.timestamp.value,
                                    amount = it.amount.rawValue
                                )
                            },
                            messageToSign = result.messageToSign,
                            signedMessage = result.signedMessage?.value,
                            createdAt = result.createdAt.value
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchAssetBalanceRequestsByProjectId() {
        val id = AssetBalanceRequestId(UUID.randomUUID())
        val projectId = ProjectId(UUID.randomUUID())
        val service = mock<AssetBalanceRequestService>()
        val result = FullAssetBalanceRequest(
            id = id,
            projectId = projectId,
            status = Status.SUCCESS,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "redirect-url",
            tokenAddress = ContractAddress("abc"),
            blockNumber = BlockNumber(BigInteger.TEN),
            requestedWalletAddress = WalletAddress("def"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            balance = AccountBalance(
                wallet = WalletAddress("def"),
                blockNumber = BlockNumber(BigInteger.TEN),
                timestamp = UtcDateTime.ofEpochSeconds(0L),
                amount = Balance(BigInteger.ONE)
            ),
            messageToSign = "message-to-sign",
            signedMessage = SignedMessage("signed-message"),
            createdAt = TestData.TIMESTAMP
        )

        suppose("some asset balance requests will be fetched by project ID") {
            call(service.getAssetBalanceRequestsByProjectId(projectId))
                .willReturn(listOf(result))
        }

        val controller = AssetBalanceRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getAssetBalanceRequestsByProjectId(projectId)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        AssetBalanceRequestsResponse(
                            listOf(
                                AssetBalanceRequestResponse(
                                    id = result.id,
                                    projectId = result.projectId,
                                    status = result.status,
                                    chainId = result.chainId.value,
                                    redirectUrl = result.redirectUrl,
                                    tokenAddress = result.tokenAddress?.rawValue,
                                    assetType = AssetType.TOKEN,
                                    blockNumber = result.blockNumber?.value,
                                    walletAddress = result.requestedWalletAddress?.rawValue,
                                    arbitraryData = result.arbitraryData,
                                    screenConfig = result.screenConfig,
                                    balance = result.balance?.let {
                                        BalanceResponse(
                                            wallet = it.wallet.rawValue,
                                            blockNumber = it.blockNumber.value,
                                            timestamp = it.timestamp.value,
                                            amount = it.amount.rawValue
                                        )
                                    },
                                    messageToSign = result.messageToSign,
                                    signedMessage = result.signedMessage?.value,
                                    createdAt = result.createdAt.value
                                )
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyAttachSignedMessage() {
        val service = mock<AssetBalanceRequestService>()
        val controller = AssetBalanceRequestController(service)

        val id = AssetBalanceRequestId(UUID.randomUUID())
        val walletAddress = WalletAddress("abc")
        val signedMessage = SignedMessage("signed-message")

        suppose("signed message will be attached") {
            val request = AttachSignedMessageRequest(walletAddress.rawValue, signedMessage.value)
            controller.attachSignedMessage(id, request)
            JsonSchemaDocumentation.createSchema(request.javaClass)
        }

        verify("signed message is correctly attached") {
            expectInteractions(service) {
                once.attachWalletAddressAndSignedMessage(id, walletAddress, signedMessage)
            }
        }
    }
}
