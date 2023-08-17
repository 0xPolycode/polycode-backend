package polycode.controller

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import polycode.JsonSchemaDocumentation
import polycode.TestBase
import polycode.TestData
import polycode.blockchain.properties.ChainSpec
import polycode.features.api.access.model.result.Project
import polycode.features.contract.deployment.model.json.ArtifactJson
import polycode.features.contract.deployment.model.json.InterfaceManifestJsonWithId
import polycode.features.contract.deployment.model.json.ManifestJson
import polycode.features.contract.deployment.model.response.ContractDeploymentRequestResponse
import polycode.features.contract.deployment.model.result.ContractDecorator
import polycode.features.contract.deployment.model.result.ContractDeploymentRequest
import polycode.features.contract.deployment.service.ContractDeploymentRequestService
import polycode.features.contract.importing.controller.ImportContractController
import polycode.features.contract.importing.model.params.ImportContractParams
import polycode.features.contract.importing.model.request.ImportContractRequest
import polycode.features.contract.importing.model.response.ImportPreviewResponse
import polycode.features.contract.importing.service.ContractImportService
import polycode.features.contract.interfaces.model.request.ImportedContractInterfacesRequest
import polycode.features.contract.interfaces.model.response.ContractInterfaceManifestResponse
import polycode.features.contract.interfaces.model.response.SuggestedContractInterfaceManifestsResponse
import polycode.features.contract.interfaces.model.result.MatchingContractInterfaces
import polycode.features.contract.interfaces.service.ContractInterfacesService
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.model.ScreenConfig
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

class ImportContractControllerTest : TestBase() {

    @Test
    fun mustCorrectlyPreviewContractImport() {
        val result = ContractDecorator(
            id = ContractId("contract-id"),
            name = "name",
            description = "description",
            binary = ContractBinaryData("00"),
            tags = emptyList(),
            implements = emptyList(),
            constructors = emptyList(),
            functions = emptyList(),
            events = emptyList(),
            manifest = ManifestJson.EMPTY,
            artifact = ArtifactJson.EMPTY
        )
        val contractAddress = ContractAddress("abc")
        val chainSpec = ChainSpec(ChainId(1337L), "test")

        val importService = mock<ContractImportService>()

        suppose("some smart contract import will be previewed") {
            call(importService.previewImport(contractAddress, chainSpec))
                .willReturn(result)
        }

        val controller = ImportContractController(importService, mock(), mock())

        verify("controller returns correct response") {
            val response = controller.previewSmartContractImport(
                chainId = chainSpec.chainId.value,
                contractAddress = contractAddress.rawValue,
                customRpcUrl = chainSpec.customRpcUrl
            )

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(ImportPreviewResponse(result)))
        }
    }

    @Test
    fun mustCorrectlyImportContract() {
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

        val importService = mock<ContractImportService>()
        val params = ImportContractParams(
            alias = result.value.alias,
            contractId = result.value.contractId,
            contractAddress = result.value.contractAddress!!,
            redirectUrl = result.value.redirectUrl,
            arbitraryData = result.value.arbitraryData,
            screenConfig = result.value.screenConfig
        )
        val projectId = ProjectId(UUID.randomUUID())
        val project = Project(
            id = projectId,
            ownerId = UserId(UUID.randomUUID()),
            baseRedirectUrl = BaseUrl("base-url"),
            chainId = ChainId(1337L),
            customRpcUrl = null,
            createdAt = TestData.TIMESTAMP
        )

        suppose("some smart contract is not already imported") {
            call(importService.importExistingContract(params, project))
                .willReturn(null)
        }

        suppose("some smart contract will be imported") {
            call(importService.importContract(params, project))
                .willReturn(result.value.id)
        }

        val deploymentService = mock<ContractDeploymentRequestService>()

        suppose("imported contract will be deployed") {
            call(deploymentService.getContractDeploymentRequest(result.value.id))
                .willReturn(result)
        }

        val controller = ImportContractController(importService, deploymentService, mock())

        verify("controller returns correct response") {
            val request = ImportContractRequest(
                alias = result.value.alias,
                contractId = result.value.contractId.value,
                contractAddress = result.value.contractAddress!!.rawValue,
                redirectUrl = result.value.redirectUrl,
                arbitraryData = result.value.arbitraryData,
                screenConfig = result.value.screenConfig
            )
            val response = controller.importSmartContract(
                project = project,
                requestBody = request
            )

            JsonSchemaDocumentation.createSchema(request.javaClass)
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
    fun mustCorrectlySuggestInterfacesForSmartContract() {
        val id = ContractDeploymentRequestId(UUID.randomUUID())
        val result = InterfaceManifestJsonWithId(
            id = InterfaceId("interface-id"),
            name = "name",
            description = "description",
            tags = emptySet(),
            matchingEventDecorators = emptyList(),
            matchingFunctionDecorators = emptyList()
        )

        val contractInterfacesService = mock<ContractInterfacesService>()

        suppose("some interfaces are suggested") {
            call(contractInterfacesService.getSuggestedInterfacesForImportedSmartContract(id))
                .willReturn(
                    MatchingContractInterfaces(
                        manifests = listOf(result),
                        bestMatchingInterfaces = listOf(result.id)
                    )
                )
        }

        val controller = ImportContractController(mock(), mock(), contractInterfacesService)

        verify("controller returns correct response") {
            val response = controller.getSuggestedInterfacesForImportedSmartContract(id)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        SuggestedContractInterfaceManifestsResponse(
                            manifests = listOf(ContractInterfaceManifestResponse(result)),
                            bestMatchingInterfaces = listOf(result.id.value)
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyAddInterfacesToImportedContract() {
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
        val project = Project(
            id = result.value.projectId,
            ownerId = UserId(UUID.randomUUID()),
            baseRedirectUrl = BaseUrl("base-url"),
            chainId = ChainId(1337L),
            customRpcUrl = null,
            createdAt = TestData.TIMESTAMP
        )

        val deploymentService = mock<ContractDeploymentRequestService>()

        suppose("imported contract will be returned") {
            call(deploymentService.getContractDeploymentRequest(result.value.id))
                .willReturn(result)
        }

        val contractInterfacesService = mock<ContractInterfacesService>()
        val newInterfaces = listOf(InterfaceId("new-interface"))

        val controller = ImportContractController(mock(), deploymentService, contractInterfacesService)

        verify("controller returns correct response") {
            val request = ImportedContractInterfacesRequest(newInterfaces.map { it.value })
            val response = controller.addInterfacesToImportedSmartContract(project, result.value.id, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
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

        verify("some interfaces are added") {
            expectInteractions(contractInterfacesService) {
                once.addInterfacesToImportedContract(result.value.id, result.value.projectId, newInterfaces)
            }
        }
    }

    @Test
    fun mustCorrectlyRemoveInterfacesFromImportedContract() {
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
        val project = Project(
            id = result.value.projectId,
            ownerId = UserId(UUID.randomUUID()),
            baseRedirectUrl = BaseUrl("base-url"),
            chainId = ChainId(1337L),
            customRpcUrl = null,
            createdAt = TestData.TIMESTAMP
        )

        val deploymentService = mock<ContractDeploymentRequestService>()

        suppose("imported contract will be returned") {
            call(deploymentService.getContractDeploymentRequest(result.value.id))
                .willReturn(result)
        }

        val contractInterfacesService = mock<ContractInterfacesService>()
        val interfacesToRemove = listOf(InterfaceId("new-interface"))

        val controller = ImportContractController(mock(), deploymentService, contractInterfacesService)

        verify("controller returns correct response") {
            val request = ImportedContractInterfacesRequest(interfacesToRemove.map { it.value })
            val response = controller.removeInterfacesFromImportedSmartContract(project, result.value.id, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
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

        verify("some interfaces are removed") {
            expectInteractions(contractInterfacesService) {
                once.removeInterfacesFromImportedContract(result.value.id, result.value.projectId, interfacesToRemove)
            }
        }
    }

    @Test
    fun mustCorrectlySetInterfacesForImportedContract() {
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
        val project = Project(
            id = result.value.projectId,
            ownerId = UserId(UUID.randomUUID()),
            baseRedirectUrl = BaseUrl("base-url"),
            chainId = ChainId(1337L),
            customRpcUrl = null,
            createdAt = TestData.TIMESTAMP
        )

        val deploymentService = mock<ContractDeploymentRequestService>()

        suppose("imported contract will be returned") {
            call(deploymentService.getContractDeploymentRequest(result.value.id))
                .willReturn(result)
        }

        val contractInterfacesService = mock<ContractInterfacesService>()
        val interfacesToSet = listOf(InterfaceId("new-interface"))

        val controller = ImportContractController(mock(), deploymentService, contractInterfacesService)

        verify("controller returns correct response") {
            val request = ImportedContractInterfacesRequest(interfacesToSet.map { it.value })
            val response = controller.setInterfacesForImportedSmartContract(project, result.value.id, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
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

        verify("some interfaces are set") {
            expectInteractions(contractInterfacesService) {
                once.setImportedContractInterfaces(result.value.id, result.value.projectId, interfacesToSet)
            }
        }
    }
}
