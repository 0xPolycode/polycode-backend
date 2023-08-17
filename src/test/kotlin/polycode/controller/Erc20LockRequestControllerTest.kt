package polycode.controller

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import polycode.JsonSchemaDocumentation
import polycode.TestBase
import polycode.TestData
import polycode.features.api.access.model.result.Project
import polycode.features.asset.lock.controller.Erc20LockRequestController
import polycode.features.asset.lock.model.params.CreateErc20LockRequestParams
import polycode.features.asset.lock.model.request.CreateErc20LockRequest
import polycode.features.asset.lock.model.response.Erc20LockRequestResponse
import polycode.features.asset.lock.model.response.Erc20LockRequestsResponse
import polycode.features.asset.lock.model.result.Erc20LockRequest
import polycode.features.asset.lock.service.Erc20LockRequestService
import polycode.generated.jooq.id.Erc20LockRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.model.ScreenConfig
import polycode.model.request.AttachTransactionInfoRequest
import polycode.model.response.TransactionResponse
import polycode.util.Balance
import polycode.util.BaseUrl
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.DurationSeconds
import polycode.util.FunctionData
import polycode.util.Status
import polycode.util.TransactionData
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import polycode.util.WithFunctionData
import polycode.util.WithTransactionData
import java.math.BigInteger
import java.util.UUID

class Erc20LockRequestControllerTest : TestBase() {

    @Test
    fun mustCorrectlyCreateErc20LockRequest() {
        val params = CreateErc20LockRequestParams(
            redirectUrl = "redirect-url",
            tokenAddress = ContractAddress("a"),
            tokenAmount = Balance(BigInteger.TEN),
            lockDuration = DurationSeconds(BigInteger.valueOf(123L)),
            lockContractAddress = ContractAddress("b"),
            tokenSenderAddress = WalletAddress("c"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        val result = Erc20LockRequest(
            id = Erc20LockRequestId(UUID.randomUUID()),
            projectId = ProjectId(UUID.randomUUID()),
            chainId = ChainId(1337L),
            redirectUrl = params.redirectUrl!!,
            tokenAddress = params.tokenAddress,
            tokenAmount = params.tokenAmount,
            lockDuration = params.lockDuration,
            lockContractAddress = params.lockContractAddress,
            tokenSenderAddress = params.tokenSenderAddress,
            txHash = null,
            arbitraryData = params.arbitraryData,
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
        val data = FunctionData("data")
        val service = mock<Erc20LockRequestService>()

        suppose("ERC20 lock request will be created") {
            call(service.createErc20LockRequest(params, project))
                .willReturn(WithFunctionData(result, data))
        }

        val controller = Erc20LockRequestController(service)

        verify("controller returns correct response") {
            val request = CreateErc20LockRequest(
                redirectUrl = params.redirectUrl,
                tokenAddress = params.tokenAddress.rawValue,
                amount = params.tokenAmount.rawValue,
                lockDurationInSeconds = params.lockDuration.rawValue,
                lockContractAddress = params.lockContractAddress.rawValue,
                senderAddress = params.tokenSenderAddress?.rawValue,
                arbitraryData = params.arbitraryData,
                screenConfig = params.screenConfig
            )
            val response = controller.createErc20LockRequest(project, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        Erc20LockRequestResponse(
                            id = result.id,
                            projectId = project.id,
                            status = Status.PENDING,
                            chainId = result.chainId.value,
                            tokenAddress = result.tokenAddress.rawValue,
                            amount = result.tokenAmount.rawValue,
                            lockDurationInSeconds = params.lockDuration.rawValue,
                            unlocksAt = null,
                            lockContractAddress = result.lockContractAddress.rawValue,
                            senderAddress = result.tokenSenderAddress?.rawValue,
                            arbitraryData = result.arbitraryData,
                            screenConfig = result.screenConfig,
                            redirectUrl = result.redirectUrl,
                            lockTx = TransactionResponse(
                                txHash = null,
                                from = result.tokenSenderAddress?.rawValue,
                                to = result.tokenAddress.rawValue,
                                data = data.value,
                                value = BigInteger.ZERO,
                                blockConfirmations = null,
                                timestamp = null
                            ),
                            createdAt = TestData.TIMESTAMP.value,
                            events = null
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchErc20LockRequest() {
        val id = Erc20LockRequestId(UUID.randomUUID())
        val service = mock<Erc20LockRequestService>()
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionData(
            value = Erc20LockRequest(
                id = id,
                projectId = ProjectId(UUID.randomUUID()),
                chainId = ChainId(123L),
                redirectUrl = "redirect-url",
                tokenAddress = ContractAddress("a"),
                tokenAmount = Balance(BigInteger.TEN),
                lockDuration = DurationSeconds(BigInteger.valueOf(123L)),
                lockContractAddress = ContractAddress("b"),
                tokenSenderAddress = WalletAddress("c"),
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                screenConfig = ScreenConfig(
                    beforeActionMessage = "before-action-message",
                    afterActionMessage = "after-action-message"
                ),
                txHash = txHash,
                createdAt = TestData.TIMESTAMP
            ),
            status = Status.SUCCESS,
            transactionData = TransactionData(
                txHash = txHash,
                fromAddress = WalletAddress("b"),
                toAddress = ContractAddress("a"),
                data = FunctionData("data"),
                value = Balance.ZERO,
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP,
                events = emptyList()
            )
        )

        suppose("some ERC20 lock request will be fetched") {
            call(service.getErc20LockRequest(id))
                .willReturn(result)
        }

        val controller = Erc20LockRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getErc20LockRequest(id)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        Erc20LockRequestResponse(
                            id = result.value.id,
                            projectId = result.value.projectId,
                            status = result.status,
                            chainId = result.value.chainId.value,
                            tokenAddress = result.value.tokenAddress.rawValue,
                            amount = result.value.tokenAmount.rawValue,
                            lockDurationInSeconds = result.value.lockDuration.rawValue,
                            unlocksAt = (TestData.TIMESTAMP + result.value.lockDuration).value,
                            lockContractAddress = result.value.lockContractAddress.rawValue,
                            senderAddress = result.value.tokenSenderAddress?.rawValue,
                            arbitraryData = result.value.arbitraryData,
                            screenConfig = result.value.screenConfig,
                            redirectUrl = result.value.redirectUrl,
                            lockTx = TransactionResponse(
                                txHash = result.transactionData.txHash?.value,
                                from = result.transactionData.fromAddress?.rawValue,
                                to = result.transactionData.toAddress.rawValue,
                                data = result.transactionData.data?.value,
                                value = BigInteger.ZERO,
                                blockConfirmations = result.transactionData.blockConfirmations,
                                timestamp = TestData.TIMESTAMP.value
                            ),
                            createdAt = result.value.createdAt.value,
                            events = emptyList()
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchErc20LockRequestsByProjectId() {
        val id = Erc20LockRequestId(UUID.randomUUID())
        val projectId = ProjectId(UUID.randomUUID())
        val service = mock<Erc20LockRequestService>()
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionData(
            value = Erc20LockRequest(
                id = id,
                projectId = projectId,
                chainId = ChainId(123L),
                redirectUrl = "redirect-url",
                tokenAddress = ContractAddress("a"),
                tokenAmount = Balance(BigInteger.TEN),
                lockDuration = DurationSeconds(BigInteger.valueOf(123L)),
                lockContractAddress = ContractAddress("b"),
                tokenSenderAddress = WalletAddress("c"),
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                screenConfig = ScreenConfig(
                    beforeActionMessage = "before-action-message",
                    afterActionMessage = "after-action-message"
                ),
                txHash = txHash,
                createdAt = TestData.TIMESTAMP
            ),
            status = Status.SUCCESS,
            transactionData = TransactionData(
                txHash = txHash,
                fromAddress = WalletAddress("b"),
                toAddress = ContractAddress("a"),
                data = FunctionData("data"),
                value = Balance.ZERO,
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP,
                events = emptyList()
            )
        )

        suppose("some ERC20 lock requests will be fetched by project ID") {
            call(service.getErc20LockRequestsByProjectId(projectId))
                .willReturn(listOf(result))
        }

        val controller = Erc20LockRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getErc20LockRequestsByProjectId(projectId)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        Erc20LockRequestsResponse(
                            listOf(
                                Erc20LockRequestResponse(
                                    id = result.value.id,
                                    projectId = result.value.projectId,
                                    status = result.status,
                                    chainId = result.value.chainId.value,
                                    tokenAddress = result.value.tokenAddress.rawValue,
                                    amount = result.value.tokenAmount.rawValue,
                                    lockDurationInSeconds = result.value.lockDuration.rawValue,
                                    unlocksAt = (TestData.TIMESTAMP + result.value.lockDuration).value,
                                    lockContractAddress = result.value.lockContractAddress.rawValue,
                                    senderAddress = result.value.tokenSenderAddress?.rawValue,
                                    arbitraryData = result.value.arbitraryData,
                                    screenConfig = result.value.screenConfig,
                                    redirectUrl = result.value.redirectUrl,
                                    lockTx = TransactionResponse(
                                        txHash = result.transactionData.txHash?.value,
                                        from = result.transactionData.fromAddress?.rawValue,
                                        to = result.transactionData.toAddress.rawValue,
                                        data = result.transactionData.data?.value,
                                        value = BigInteger.ZERO,
                                        blockConfirmations = result.transactionData.blockConfirmations,
                                        timestamp = TestData.TIMESTAMP.value
                                    ),
                                    createdAt = result.value.createdAt.value,
                                    events = emptyList()
                                )
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyAttachTransactionInfo() {
        val service = mock<Erc20LockRequestService>()
        val controller = Erc20LockRequestController(service)

        val id = Erc20LockRequestId(UUID.randomUUID())
        val txHash = "tx-hash"
        val caller = "c"

        suppose("transaction info will be attached") {
            val request = AttachTransactionInfoRequest(txHash, caller)
            controller.attachTransactionInfo(id, request)
            JsonSchemaDocumentation.createSchema(request.javaClass)
        }

        verify("transaction info is correctly attached") {
            expectInteractions(service) {
                once.attachTxInfo(id, TransactionHash(txHash), WalletAddress(caller))
            }
        }
    }
}
