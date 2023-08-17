package polycode.controller

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import polycode.JsonSchemaDocumentation
import polycode.TestBase
import polycode.TestData
import polycode.features.api.access.model.result.Project
import polycode.features.contract.deployment.controller.ContractDeploymentRequestController
import polycode.features.contract.deployment.model.filters.ContractDeploymentRequestFilters
import polycode.features.contract.deployment.model.params.CreateContractDeploymentRequestParams
import polycode.features.contract.deployment.model.request.CreateContractDeploymentRequest
import polycode.features.contract.deployment.model.response.ContractDeploymentRequestResponse
import polycode.features.contract.deployment.model.response.ContractDeploymentRequestsResponse
import polycode.features.contract.deployment.model.result.ContractDeploymentRequest
import polycode.features.contract.deployment.service.ContractDeploymentRequestService
import polycode.features.functions.encoding.model.FunctionArgument
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.model.ScreenConfig
import polycode.model.filters.AndList
import polycode.model.filters.OrList
import polycode.model.request.AttachTransactionInfoRequest
import polycode.model.response.TransactionResponse
import polycode.util.Balance
import polycode.util.BaseUrl
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.ContractBinaryData
import polycode.util.ContractId
import polycode.util.ContractTag
import polycode.util.FunctionData
import polycode.util.InterfaceId
import polycode.util.Status
import polycode.util.TransactionData
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import polycode.util.WithTransactionData
import polycode.util.ZeroAddress
import java.math.BigInteger
import java.util.UUID

class ContractDeploymentRequestControllerTest : TestBase() {

    @Test
    fun mustCorrectlyCreateContractDeploymentRequest() {
        val params = CreateContractDeploymentRequestParams(
            alias = "alias",
            contractId = ContractId("contract-id"),
            constructorParams = listOf(FunctionArgument(WalletAddress("abc"))),
            deployerAddress = WalletAddress("a"),
            initialEthAmount = Balance(BigInteger.TEN),
            redirectUrl = "redirect-url",
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        val result = ContractDeploymentRequest(
            id = ContractDeploymentRequestId(UUID.randomUUID()),
            alias = params.alias,
            name = "name",
            description = "description",
            contractId = params.contractId,
            contractData = ContractBinaryData("00"),
            constructorParams = TestData.EMPTY_JSON_ARRAY,
            contractTags = listOf(ContractTag("contract-tag")),
            contractImplements = listOf(InterfaceId("contract-trait")),
            initialEthAmount = params.initialEthAmount,
            chainId = ChainId(1337),
            redirectUrl = params.redirectUrl!!,
            projectId = ProjectId(UUID.randomUUID()),
            createdAt = TestData.TIMESTAMP,
            arbitraryData = params.arbitraryData,
            screenConfig = params.screenConfig,
            contractAddress = ContractAddress("cafebabe"),
            deployerAddress = params.deployerAddress,
            txHash = null,
            imported = false,
            proxy = false,
            implementationContractAddress = null
        )
        val project = Project(
            id = result.projectId,
            ownerId = UserId(UUID.randomUUID()),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = result.chainId,
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        val service = mock<ContractDeploymentRequestService>()

        suppose("contract deployment request will be created") {
            call(service.createContractDeploymentRequest(params, project))
                .willReturn(result)
        }

        val controller = ContractDeploymentRequestController(service)

        verify("controller returns correct response") {
            val request = CreateContractDeploymentRequest(
                alias = params.alias,
                contractId = params.contractId.value,
                constructorParams = params.constructorParams,
                deployerAddress = params.deployerAddress?.rawValue,
                initialEthAmount = params.initialEthAmount.rawValue,
                redirectUrl = params.redirectUrl,
                arbitraryData = params.arbitraryData,
                screenConfig = params.screenConfig
            )
            val response = controller.createContractDeploymentRequest(project, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractDeploymentRequestResponse(
                            id = result.id,
                            alias = params.alias,
                            name = result.name,
                            description = result.description,
                            status = Status.PENDING,
                            contractId = result.contractId.value,
                            contractDeploymentData = result.contractData.withPrefix,
                            constructorParams = TestData.EMPTY_JSON_ARRAY,
                            contractTags = result.contractTags.map { it.value },
                            contractImplements = result.contractImplements.map { it.value },
                            initialEthAmount = result.initialEthAmount.rawValue,
                            chainId = result.chainId.value,
                            redirectUrl = result.redirectUrl,
                            projectId = result.projectId,
                            createdAt = result.createdAt.value,
                            arbitraryData = result.arbitraryData,
                            screenConfig = params.screenConfig,
                            contractAddress = result.contractAddress?.rawValue,
                            deployerAddress = result.deployerAddress?.rawValue,
                            deployTx = TransactionResponse(
                                txHash = null,
                                from = result.deployerAddress?.rawValue,
                                to = ZeroAddress.rawValue,
                                data = result.contractData.withPrefix,
                                value = result.initialEthAmount.rawValue,
                                blockConfirmations = null,
                                timestamp = null
                            ),
                            imported = result.imported,
                            proxy = false,
                            implementationContractAddress = null,
                            events = null
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyMarkContractDeploymentRequestAsDeleted() {
        val id = ContractDeploymentRequestId(UUID.randomUUID())
        val project = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = UserId(UUID.randomUUID()),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = ChainId(1337L),
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        val service = mock<ContractDeploymentRequestService>()

        val controller = ContractDeploymentRequestController(service)

        verify("correct service call is made") {
            controller.markContractDeploymentRequestAsDeleted(project, id)

            expectInteractions(service) {
                once.markContractDeploymentRequestAsDeleted(id, project.id)
            }
        }
    }

    @Test
    fun mustCorrectlyFetchContractDeploymentRequest() {
        val id = ContractDeploymentRequestId(UUID.randomUUID())
        val service = mock<ContractDeploymentRequestService>()
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionData(
            value = ContractDeploymentRequest(
                id = id,
                alias = "alias",
                name = "name",
                description = "description",
                contractId = ContractId("contract-id"),
                contractData = ContractBinaryData("00"),
                constructorParams = TestData.EMPTY_JSON_ARRAY,
                contractTags = listOf(ContractTag("contract-tag")),
                contractImplements = listOf(InterfaceId("contract-trait")),
                initialEthAmount = Balance(BigInteger.TEN),
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
                deployerAddress = WalletAddress("a"),
                txHash = txHash,
                imported = false,
                proxy = false,
                implementationContractAddress = null
            ),
            status = Status.SUCCESS,
            transactionData = TransactionData(
                txHash = txHash,
                fromAddress = WalletAddress("b"),
                toAddress = ZeroAddress,
                data = FunctionData("00"),
                value = Balance(BigInteger.TEN),
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP,
                events = emptyList()
            )
        )

        suppose("some contract deployment request will be fetched") {
            call(service.getContractDeploymentRequest(id))
                .willReturn(result)
        }

        val controller = ContractDeploymentRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getContractDeploymentRequest(id)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractDeploymentRequestResponse(
                            id = result.value.id,
                            alias = result.value.alias,
                            name = result.value.name,
                            description = result.value.description,
                            status = result.status,
                            contractId = result.value.contractId.value,
                            contractDeploymentData = result.value.contractData.withPrefix,
                            constructorParams = TestData.EMPTY_JSON_ARRAY,
                            contractTags = result.value.contractTags.map { it.value },
                            contractImplements = result.value.contractImplements.map { it.value },
                            initialEthAmount = result.value.initialEthAmount.rawValue,
                            chainId = result.value.chainId.value,
                            redirectUrl = result.value.redirectUrl,
                            projectId = result.value.projectId,
                            createdAt = result.value.createdAt.value,
                            arbitraryData = result.value.arbitraryData,
                            screenConfig = result.value.screenConfig.orEmpty(),
                            contractAddress = result.value.contractAddress?.rawValue,
                            deployerAddress = result.value.deployerAddress?.rawValue,
                            deployTx = TransactionResponse(
                                txHash = result.transactionData.txHash?.value,
                                from = result.transactionData.fromAddress?.rawValue,
                                to = result.transactionData.toAddress.rawValue,
                                data = result.transactionData.data?.value,
                                value = result.transactionData.value.rawValue,
                                blockConfirmations = result.transactionData.blockConfirmations,
                                timestamp = result.transactionData.timestamp?.value
                            ),
                            imported = result.value.imported,
                            proxy = false,
                            implementationContractAddress = null,
                            events = emptyList()
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchContractDeploymentRequestsByProjectIdAndFilters() {
        val projectId = ProjectId(UUID.randomUUID())
        val service = mock<ContractDeploymentRequestService>()
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionData(
            value = ContractDeploymentRequest(
                id = ContractDeploymentRequestId(UUID.randomUUID()),
                alias = "alias",
                name = "name",
                description = "description",
                contractId = ContractId("contract-id"),
                contractData = ContractBinaryData("00"),
                constructorParams = TestData.EMPTY_JSON_ARRAY,
                contractTags = listOf(ContractTag("contract-tag")),
                contractImplements = listOf(InterfaceId("contract-trait")),
                initialEthAmount = Balance(BigInteger.TEN),
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
                deployerAddress = WalletAddress("a"),
                txHash = txHash,
                imported = false,
                proxy = false,
                implementationContractAddress = null
            ),
            status = Status.SUCCESS,
            transactionData = TransactionData(
                txHash = txHash,
                fromAddress = WalletAddress("b"),
                toAddress = ZeroAddress,
                data = FunctionData("00"),
                value = Balance(BigInteger.TEN),
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP,
                events = emptyList()
            )
        )

        val filters = ContractDeploymentRequestFilters(
            contractIds = OrList(ContractId("contract-id")),
            contractTags = OrList(AndList(ContractTag("tag-1"), ContractTag("tag-2"))),
            contractImplements = OrList(AndList(InterfaceId("trait-1"), InterfaceId("trait-2"))),
            deployedOnly = true
        )

        suppose("some contract deployment requests will be fetched by project ID and filters") {
            call(service.getContractDeploymentRequestsByProjectIdAndFilters(projectId, filters))
                .willReturn(listOf(result))
        }

        val controller = ContractDeploymentRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getContractDeploymentRequestsByProjectIdAndFilters(
                projectId = projectId,
                contractIds = listOf("contract-id"),
                contractTags = listOf("tag-1 AND tag-2"),
                contractImplements = listOf("trait-1 AND trait-2"),
                deployedOnly = true
            )

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractDeploymentRequestsResponse(
                            listOf(
                                ContractDeploymentRequestResponse(
                                    id = result.value.id,
                                    alias = result.value.alias,
                                    name = result.value.name,
                                    description = result.value.description,
                                    status = result.status,
                                    contractId = result.value.contractId.value,
                                    contractDeploymentData = result.value.contractData.withPrefix,
                                    constructorParams = TestData.EMPTY_JSON_ARRAY,
                                    contractTags = result.value.contractTags.map { it.value },
                                    contractImplements = result.value.contractImplements.map { it.value },
                                    initialEthAmount = result.value.initialEthAmount.rawValue,
                                    chainId = result.value.chainId.value,
                                    redirectUrl = result.value.redirectUrl,
                                    projectId = result.value.projectId,
                                    createdAt = result.value.createdAt.value,
                                    arbitraryData = result.value.arbitraryData,
                                    screenConfig = result.value.screenConfig.orEmpty(),
                                    contractAddress = result.value.contractAddress?.rawValue,
                                    deployerAddress = result.value.deployerAddress?.rawValue,
                                    deployTx = TransactionResponse(
                                        txHash = result.transactionData.txHash?.value,
                                        from = result.transactionData.fromAddress?.rawValue,
                                        to = result.transactionData.toAddress.rawValue,
                                        data = result.transactionData.data?.value,
                                        value = result.transactionData.value.rawValue,
                                        blockConfirmations = result.transactionData.blockConfirmations,
                                        timestamp = result.transactionData.timestamp?.value
                                    ),
                                    imported = result.value.imported,
                                    proxy = false,
                                    implementationContractAddress = null,
                                    events = emptyList()
                                )
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchContractDeploymentRequestByProjectIdAndAlias() {
        val projectId = ProjectId(UUID.randomUUID())
        val alias = "alias"
        val service = mock<ContractDeploymentRequestService>()
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionData(
            value = ContractDeploymentRequest(
                id = ContractDeploymentRequestId(UUID.randomUUID()),
                alias = alias,
                name = "name",
                description = "description",
                contractId = ContractId("contract-id"),
                contractData = ContractBinaryData("00"),
                constructorParams = TestData.EMPTY_JSON_ARRAY,
                contractTags = listOf(ContractTag("contract-tag")),
                contractImplements = listOf(InterfaceId("contract-trait")),
                initialEthAmount = Balance(BigInteger.TEN),
                chainId = ChainId(1337),
                redirectUrl = "redirect-url",
                projectId = projectId,
                createdAt = TestData.TIMESTAMP,
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                screenConfig = ScreenConfig(
                    beforeActionMessage = "before-action-message",
                    afterActionMessage = "after-action-message"
                ),
                contractAddress = ContractAddress("cafebabe"),
                deployerAddress = WalletAddress("a"),
                txHash = txHash,
                imported = false,
                proxy = false,
                implementationContractAddress = null
            ),
            status = Status.SUCCESS,
            transactionData = TransactionData(
                txHash = txHash,
                fromAddress = WalletAddress("b"),
                toAddress = ZeroAddress,
                data = FunctionData("00"),
                value = Balance(BigInteger.TEN),
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP,
                events = emptyList()
            )
        )

        suppose("some contract deployment request will be fetched") {
            call(service.getContractDeploymentRequestByProjectIdAndAlias(projectId, alias))
                .willReturn(result)
        }

        val controller = ContractDeploymentRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getContractDeploymentRequestByProjectIdAndAlias(projectId, alias)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        ContractDeploymentRequestResponse(
                            id = result.value.id,
                            alias = result.value.alias,
                            name = result.value.name,
                            description = result.value.description,
                            status = result.status,
                            contractId = result.value.contractId.value,
                            contractDeploymentData = result.value.contractData.withPrefix,
                            constructorParams = TestData.EMPTY_JSON_ARRAY,
                            contractTags = result.value.contractTags.map { it.value },
                            contractImplements = result.value.contractImplements.map { it.value },
                            initialEthAmount = result.value.initialEthAmount.rawValue,
                            chainId = result.value.chainId.value,
                            redirectUrl = result.value.redirectUrl,
                            projectId = result.value.projectId,
                            createdAt = result.value.createdAt.value,
                            arbitraryData = result.value.arbitraryData,
                            screenConfig = result.value.screenConfig.orEmpty(),
                            contractAddress = result.value.contractAddress?.rawValue,
                            deployerAddress = result.value.deployerAddress?.rawValue,
                            deployTx = TransactionResponse(
                                txHash = result.transactionData.txHash?.value,
                                from = result.transactionData.fromAddress?.rawValue,
                                to = result.transactionData.toAddress.rawValue,
                                data = result.transactionData.data?.value,
                                value = result.transactionData.value.rawValue,
                                blockConfirmations = result.transactionData.blockConfirmations,
                                timestamp = result.transactionData.timestamp?.value
                            ),
                            imported = result.value.imported,
                            proxy = false,
                            implementationContractAddress = null,
                            events = emptyList()
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyAttachTransactionInfo() {
        val service = mock<ContractDeploymentRequestService>()
        val controller = ContractDeploymentRequestController(service)

        val id = ContractDeploymentRequestId(UUID.randomUUID())
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
