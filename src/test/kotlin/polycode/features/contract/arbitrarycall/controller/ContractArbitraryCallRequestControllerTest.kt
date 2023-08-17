package polycode.features.contract.arbitrarycall.controller

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import polycode.JsonSchemaDocumentation
import polycode.TestBase
import polycode.TestData
import polycode.features.api.access.model.result.Project
import polycode.features.contract.arbitrarycall.model.filters.ContractArbitraryCallRequestFilters
import polycode.features.contract.arbitrarycall.model.params.CreateContractArbitraryCallRequestParams
import polycode.features.contract.arbitrarycall.model.request.CreateContractArbitraryCallRequest
import polycode.features.contract.arbitrarycall.model.response.ContractArbitraryCallRequestResponse
import polycode.features.contract.arbitrarycall.model.response.ContractArbitraryCallRequestsResponse
import polycode.features.contract.arbitrarycall.model.result.ContractArbitraryCallRequest
import polycode.features.contract.arbitrarycall.service.ContractArbitraryCallRequestService
import polycode.features.contract.deployment.model.params.DeployedContractIdIdentifier
import polycode.generated.jooq.id.ContractArbitraryCallRequestId
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.model.ScreenConfig
import polycode.model.request.AttachTransactionInfoRequest
import polycode.model.response.TransactionResponse
import polycode.util.Balance
import polycode.util.BaseUrl
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.FunctionData
import polycode.util.Status
import polycode.util.TransactionData
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import polycode.util.WithTransactionData
import java.math.BigInteger
import java.util.UUID

class ContractArbitraryCallRequestControllerTest : TestBase() {

    @Test
    fun mustCorrectlyCreateContractArbitraryCallRequest() {
        val deployedContractId = ContractDeploymentRequestId(UUID.randomUUID())
        val params = CreateContractArbitraryCallRequestParams(
            identifier = DeployedContractIdIdentifier(deployedContractId),
            functionData = FunctionData("00"),
            ethAmount = Balance(BigInteger.TEN),
            redirectUrl = "redirect-url",
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            callerAddress = WalletAddress("a")
        )
        val result = ContractArbitraryCallRequest(
            id = ContractArbitraryCallRequestId(UUID.randomUUID()),
            deployedContractId = deployedContractId,
            chainId = ChainId(1337),
            redirectUrl = "redirect-url",
            projectId = ProjectId(UUID.randomUUID()),
            createdAt = TestData.TIMESTAMP,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            contractAddress = ContractAddress("cafebabe"),
            txHash = TransactionHash("tx-hash"),
            functionData = FunctionData("00"),
            functionName = "test",
            functionParams = TestData.EMPTY_JSON_ARRAY,
            ethAmount = Balance(BigInteger.TEN),
            callerAddress = WalletAddress("a")
        )
        val project = Project(
            id = result.projectId,
            ownerId = UserId(UUID.randomUUID()),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = result.chainId,
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        val service = mock<ContractArbitraryCallRequestService>()

        suppose("contract arbitrary call request will be created") {
            call(service.createContractArbitraryCallRequest(params, project))
                .willReturn(result)
        }

        val controller = ContractArbitraryCallRequestController(service)

        verify("controller returns correct response") {
            val request = CreateContractArbitraryCallRequest(
                deployedContractId = deployedContractId,
                deployedContractAlias = null,
                contractAddress = null,
                functionData = params.functionData.value,
                ethAmount = params.ethAmount.rawValue,
                redirectUrl = params.redirectUrl,
                arbitraryData = params.arbitraryData,
                screenConfig = params.screenConfig,
                callerAddress = params.callerAddress?.rawValue
            )
            val response = controller.createContractArbitraryCallRequest(project, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractArbitraryCallRequestResponse(
                            id = result.id,
                            status = Status.PENDING,
                            chainId = result.chainId.value,
                            redirectUrl = result.redirectUrl,
                            projectId = result.projectId,
                            createdAt = result.createdAt.value,
                            arbitraryData = result.arbitraryData,
                            screenConfig = result.screenConfig.orEmpty(),
                            contractAddress = result.contractAddress.rawValue,
                            deployedContractId = result.deployedContractId,
                            functionName = result.functionName,
                            functionParams = TestData.EMPTY_JSON_ARRAY,
                            functionCallData = result.functionData.value,
                            ethAmount = result.ethAmount.rawValue,
                            callerAddress = result.callerAddress?.rawValue,
                            arbitraryCallTx = TransactionResponse(
                                txHash = null,
                                from = result.callerAddress?.rawValue,
                                to = result.contractAddress.rawValue,
                                data = result.functionData.value,
                                value = result.ethAmount.rawValue,
                                blockConfirmations = null,
                                timestamp = null
                            ),
                            events = null
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchContractArbitraryCallRequest() {
        val id = ContractArbitraryCallRequestId(UUID.randomUUID())
        val service = mock<ContractArbitraryCallRequestService>()
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionData(
            value = ContractArbitraryCallRequest(
                id = id,
                deployedContractId = ContractDeploymentRequestId(UUID.randomUUID()),
                chainId = ChainId(1337),
                redirectUrl = "redirect-url",
                projectId = ProjectId(UUID.randomUUID()),
                createdAt = TestData.TIMESTAMP,
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                screenConfig = ScreenConfig(
                    beforeActionMessage = "before-action-message",
                    afterActionMessage = "after-action-message"
                ),
                contractAddress = ContractAddress("cafebabe"),
                txHash = txHash,
                functionName = "test",
                functionParams = TestData.EMPTY_JSON_ARRAY,
                functionData = FunctionData("00"),
                ethAmount = Balance(BigInteger.TEN),
                callerAddress = WalletAddress("a")
            ),
            status = Status.SUCCESS,
            transactionData = TransactionData(
                txHash = txHash,
                fromAddress = WalletAddress("b"),
                toAddress = ContractAddress("cafebabe"),
                data = FunctionData("00"),
                value = Balance(BigInteger.TEN),
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP,
                events = emptyList()
            )
        )

        suppose("some contract arbitrary call request will be fetched") {
            call(service.getContractArbitraryCallRequest(id))
                .willReturn(result)
        }

        val controller = ContractArbitraryCallRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getContractArbitraryCallRequest(id)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractArbitraryCallRequestResponse(
                            id = result.value.id,
                            status = result.status,
                            chainId = result.value.chainId.value,
                            redirectUrl = result.value.redirectUrl,
                            projectId = result.value.projectId,
                            createdAt = result.value.createdAt.value,
                            arbitraryData = result.value.arbitraryData,
                            screenConfig = result.value.screenConfig.orEmpty(),
                            contractAddress = result.value.contractAddress.rawValue,
                            deployedContractId = result.value.deployedContractId,
                            functionName = result.value.functionName,
                            functionParams = TestData.EMPTY_JSON_ARRAY,
                            functionCallData = result.value.functionData.value,
                            ethAmount = result.value.ethAmount.rawValue,
                            callerAddress = result.value.callerAddress?.rawValue,
                            arbitraryCallTx = TransactionResponse(
                                txHash = result.transactionData.txHash?.value,
                                from = result.transactionData.fromAddress?.rawValue,
                                to = result.transactionData.toAddress.rawValue,
                                data = result.transactionData.data?.value,
                                value = result.transactionData.value.rawValue,
                                blockConfirmations = result.transactionData.blockConfirmations,
                                timestamp = result.transactionData.timestamp?.value
                            ),
                            events = emptyList()
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchContractArbitraryCallRequestsByProjectIdAndFilters() {
        val projectId = ProjectId(UUID.randomUUID())
        val service = mock<ContractArbitraryCallRequestService>()
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionData(
            value = ContractArbitraryCallRequest(
                id = ContractArbitraryCallRequestId(UUID.randomUUID()),
                deployedContractId = ContractDeploymentRequestId(UUID.randomUUID()),
                chainId = ChainId(1337),
                redirectUrl = "redirect-url",
                projectId = ProjectId(UUID.randomUUID()),
                createdAt = TestData.TIMESTAMP,
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                screenConfig = ScreenConfig(
                    beforeActionMessage = "before-action-message",
                    afterActionMessage = "after-action-message"
                ),
                contractAddress = ContractAddress("cafebabe"),
                txHash = txHash,
                functionName = "test",
                functionParams = TestData.EMPTY_JSON_ARRAY,
                functionData = FunctionData("00"),
                ethAmount = Balance(BigInteger.TEN),
                callerAddress = WalletAddress("a")
            ),
            status = Status.SUCCESS,
            transactionData = TransactionData(
                txHash = txHash,
                fromAddress = WalletAddress("b"),
                toAddress = ContractAddress("cafebabe"),
                data = FunctionData("00"),
                value = Balance(BigInteger.TEN),
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP,
                events = emptyList()
            )
        )

        val filters = ContractArbitraryCallRequestFilters(
            deployedContractId = result.value.deployedContractId,
            contractAddress = result.value.contractAddress
        )

        suppose("some contract arbitrary call requests will be fetched by project ID and filters") {
            call(service.getContractArbitraryCallRequestsByProjectIdAndFilters(projectId, filters))
                .willReturn(listOf(result))
        }

        val controller = ContractArbitraryCallRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getContractArbitraryCallRequestsByProjectIdAndFilters(
                projectId = projectId,
                deployedContractId = filters.deployedContractId,
                contractAddress = filters.contractAddress?.rawValue
            )

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractArbitraryCallRequestsResponse(
                            listOf(
                                ContractArbitraryCallRequestResponse(
                                    id = result.value.id,
                                    status = result.status,
                                    chainId = result.value.chainId.value,
                                    redirectUrl = result.value.redirectUrl,
                                    projectId = result.value.projectId,
                                    createdAt = result.value.createdAt.value,
                                    arbitraryData = result.value.arbitraryData,
                                    screenConfig = result.value.screenConfig.orEmpty(),
                                    contractAddress = result.value.contractAddress.rawValue,
                                    deployedContractId = result.value.deployedContractId,
                                    functionName = result.value.functionName,
                                    functionParams = TestData.EMPTY_JSON_ARRAY,
                                    functionCallData = result.value.functionData.value,
                                    ethAmount = result.value.ethAmount.rawValue,
                                    callerAddress = result.value.callerAddress?.rawValue,
                                    arbitraryCallTx = TransactionResponse(
                                        txHash = result.transactionData.txHash?.value,
                                        from = result.transactionData.fromAddress?.rawValue,
                                        to = result.transactionData.toAddress.rawValue,
                                        data = result.transactionData.data?.value,
                                        value = result.transactionData.value.rawValue,
                                        blockConfirmations = result.transactionData.blockConfirmations,
                                        timestamp = result.transactionData.timestamp?.value
                                    ),
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
        val service = mock<ContractArbitraryCallRequestService>()
        val controller = ContractArbitraryCallRequestController(service)

        val id = ContractArbitraryCallRequestId(UUID.randomUUID())
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
