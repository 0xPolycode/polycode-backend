package polycode.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import polycode.TestBase
import polycode.TestData
import polycode.exception.ContractInterfaceNotFoundException
import polycode.exception.ResourceNotFoundException
import polycode.features.contract.deployment.model.json.AbiInputOutput
import polycode.features.contract.deployment.model.json.AbiObject
import polycode.features.contract.deployment.model.json.ArtifactJson
import polycode.features.contract.deployment.model.json.EventDecorator
import polycode.features.contract.deployment.model.json.FunctionDecorator
import polycode.features.contract.deployment.model.json.InterfaceManifestJson
import polycode.features.contract.deployment.model.json.InterfaceManifestJsonWithId
import polycode.features.contract.deployment.model.json.ManifestJson
import polycode.features.contract.deployment.model.result.ContractDecorator
import polycode.features.contract.deployment.model.result.ContractDeploymentRequest
import polycode.features.contract.deployment.repository.ContractDeploymentRequestRepository
import polycode.features.contract.deployment.repository.ContractMetadataRepository
import polycode.features.contract.deployment.repository.ImportedContractDecoratorRepository
import polycode.features.contract.interfaces.model.result.MatchingContractInterfaces
import polycode.features.contract.interfaces.repository.ContractInterfacesRepository
import polycode.features.contract.interfaces.service.ContractInterfacesServiceImpl
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.model.ScreenConfig
import polycode.util.Balance
import polycode.util.ContractAddress
import polycode.util.ContractBinaryData
import polycode.util.ContractId
import polycode.util.InterfaceId
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import java.util.UUID

class ContractInterfacesServiceTest : TestBase() {

    companion object {
        private val ID = ContractDeploymentRequestId(UUID.randomUUID())
        private val PROJECT_ID = ProjectId(UUID.randomUUID())
        private val CONTRACT_ID = ContractId("contract-id")
        private val CONTRACT_DEPLOYMENT_REQUEST = ContractDeploymentRequest(
            id = ID,
            alias = "alias",
            name = "name",
            description = "description",
            contractId = CONTRACT_ID,
            contractData = ContractBinaryData.invoke("00"),
            constructorParams = TestData.EMPTY_JSON_ARRAY,
            contractTags = emptyList(),
            contractImplements = emptyList(),
            initialEthAmount = Balance.ZERO,
            chainId = TestData.CHAIN_ID,
            redirectUrl = "redirect-url",
            projectId = PROJECT_ID,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig.EMPTY,
            contractAddress = ContractAddress("a"),
            deployerAddress = WalletAddress("b"),
            txHash = TransactionHash("tx-hash"),
            imported = true,
            proxy = false,
            implementationContractAddress = null
        )
        private val MANIFEST_JSON = ManifestJson(
            name = "name",
            description = "description",
            tags = emptySet(),
            implements = setOf("already-implemented"),
            eventDecorators = listOf(
                EventDecorator(
                    signature = "Event(string)",
                    name = "name",
                    description = "description",
                    parameterDecorators = emptyList()
                )
            ),
            constructorDecorators = emptyList(),
            functionDecorators = listOf(
                FunctionDecorator(
                    signature = "function(string)",
                    name = "name",
                    description = "description",
                    parameterDecorators = emptyList(),
                    returnDecorators = emptyList(),
                    emittableEvents = emptyList(),
                    readOnly = false
                )
            )
        )
        private val ARTIFACT_JSON = ArtifactJson(
            contractName = "Name",
            sourceName = "Name.sol",
            abi = listOf(
                AbiObject(
                    anonymous = null,
                    inputs = listOf(
                        AbiInputOutput(
                            components = null,
                            internalType = "string",
                            name = "arg0",
                            type = "string",
                            indexed = null
                        )
                    ),
                    outputs = emptyList(),
                    stateMutability = null,
                    name = "Event",
                    type = "event"
                ),
                AbiObject(
                    anonymous = null,
                    inputs = listOf(
                        AbiInputOutput(
                            components = null,
                            internalType = "string",
                            name = "arg0",
                            type = "string",
                            indexed = null
                        )
                    ),
                    outputs = emptyList(),
                    stateMutability = null,
                    name = "function",
                    type = "function"
                )
            ),
            bytecode = "",
            deployedBytecode = "",
            linkReferences = null,
            deployedLinkReferences = null
        )
        private val EMPTY_CONTRACT_INTERFACE = InterfaceManifestJson(null, null, emptySet(), emptyList(), emptyList())
        private val MULTI_FUNCTION_MANIFEST_JSON = ManifestJson(
            name = "name",
            description = "description",
            tags = emptySet(),
            implements = setOf("already-implemented"),
            eventDecorators = listOf(
                EventDecorator(
                    signature = "Event(string)",
                    name = "name",
                    description = "description",
                    parameterDecorators = emptyList()
                )
            ),
            constructorDecorators = emptyList(),
            functionDecorators = listOf(
                FunctionDecorator(
                    signature = "function(string)",
                    name = "name",
                    description = "description",
                    parameterDecorators = emptyList(),
                    returnDecorators = emptyList(),
                    emittableEvents = emptyList(),
                    readOnly = false
                ),
                FunctionDecorator(
                    signature = "another(address)",
                    name = "another",
                    description = "another description",
                    parameterDecorators = emptyList(),
                    returnDecorators = emptyList(),
                    emittableEvents = emptyList(),
                    readOnly = false
                ),
                FunctionDecorator(
                    signature = "test(uint,int)",
                    name = "test",
                    description = "test fn description",
                    parameterDecorators = emptyList(),
                    returnDecorators = emptyList(),
                    emittableEvents = emptyList(),
                    readOnly = false
                )
            )
        )
        private val MULTI_FUNCTION_ARTIFACT_JSON = ArtifactJson(
            contractName = "Name",
            sourceName = "Name.sol",
            abi = listOf(
                AbiObject(
                    anonymous = null,
                    inputs = listOf(
                        AbiInputOutput(
                            components = null,
                            internalType = "string",
                            name = "arg0",
                            type = "string",
                            indexed = null
                        )
                    ),
                    outputs = emptyList(),
                    stateMutability = null,
                    name = "Event",
                    type = "event"
                ),
                AbiObject(
                    anonymous = null,
                    inputs = listOf(
                        AbiInputOutput(
                            components = null,
                            internalType = "string",
                            name = "arg0",
                            type = "string",
                            indexed = null
                        )
                    ),
                    outputs = emptyList(),
                    stateMutability = null,
                    name = "function",
                    type = "function"
                ),
                AbiObject(
                    anonymous = null,
                    inputs = listOf(
                        AbiInputOutput(
                            components = null,
                            internalType = "address",
                            name = "arg0",
                            type = "address",
                            indexed = null
                        )
                    ),
                    outputs = emptyList(),
                    stateMutability = null,
                    name = "another",
                    type = "function"
                ),
                AbiObject(
                    anonymous = null,
                    inputs = listOf(
                        AbiInputOutput(
                            components = null,
                            internalType = "uint",
                            name = "arg0",
                            type = "uint",
                            indexed = null
                        ),
                        AbiInputOutput(
                            components = null,
                            internalType = "int",
                            name = "arg0",
                            type = "int",
                            indexed = null
                        )
                    ),
                    outputs = emptyList(),
                    stateMutability = null,
                    name = "test",
                    type = "function"
                )
            ),
            bytecode = "",
            deployedBytecode = "",
            linkReferences = null,
            deployedLinkReferences = null
        )
    }

    @Test
    fun mustCorrectlyAttachInterfacesToDecorator() {
        val contractInterfacesRepository = mock<ContractInterfacesRepository>()

        suppose("some partially matching contract interfaces will be returned") {
            call(
                contractInterfacesRepository.getAllWithPartiallyMatchingInterfaces(
                    abiFunctionSignatures = setOf("function(string)"),
                    abiEventSignatures = setOf("Event(string)")
                )
            )
                .willReturn(
                    listOf(
                        InterfaceManifestJsonWithId(
                            id = InterfaceId("already-implemented"),
                            name = "Already Implemented",
                            description = "Already Implemented",
                            tags = emptySet(),
                            matchingEventDecorators = MANIFEST_JSON.eventDecorators,
                            matchingFunctionDecorators = emptyList()
                        ),
                        InterfaceManifestJsonWithId(
                            id = InterfaceId("not-yet-implemented"),
                            name = "Not Yet Implemented",
                            description = "Not Yet Implemented",
                            tags = emptySet(),
                            matchingEventDecorators = emptyList(),
                            matchingFunctionDecorators = MANIFEST_JSON.functionDecorators
                        )
                    )
                )
        }

        suppose("some interfaces are in the contract interfaces repository") {
            call(contractInterfacesRepository.getById(anyValueClass(InterfaceId(""))))
                .willReturn(EMPTY_CONTRACT_INTERFACE)
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = mock(),
            importedContractDecoratorRepository = mock(),
            contractInterfacesRepository = contractInterfacesRepository,
            contractMetadataRepository = mock()
        )

        val decorator = ContractDecorator(
            id = ContractId("test"),
            artifact = ARTIFACT_JSON,
            manifest = MANIFEST_JSON,
            imported = true,
            interfacesProvider = contractInterfacesRepository::getById
        )

        verify("correct interfaces are attached") {
            expectThat(service.attachMatchingInterfacesToDecorator(decorator))
                .isEqualTo(
                    ContractDecorator(
                        id = decorator.id,
                        artifact = ARTIFACT_JSON,
                        manifest = MANIFEST_JSON.copy(implements = setOf("already-implemented", "not-yet-implemented")),
                        imported = true,
                        interfacesProvider = contractInterfacesRepository::getById
                    )
                )
        }
    }

    @Test
    fun mustCorrectlySuggestInterfacesForImportedContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some imported contract deployment request will be returned") {
            call(contractDeploymentRequestRepository.getById(ID))
                .willReturn(CONTRACT_DEPLOYMENT_REQUEST)
        }

        val importedContractDecoratorRepository = mock<ImportedContractDecoratorRepository>()

        suppose("some imported contract manifest will be returned") {
            call(importedContractDecoratorRepository.getManifestJsonByContractIdAndProjectId(CONTRACT_ID, PROJECT_ID))
                .willReturn(MANIFEST_JSON)
        }

        val contractInterfacesRepository = mock<ContractInterfacesRepository>()

        suppose("some partially matching contract interfaces will be returned") {
            call(
                contractInterfacesRepository.getAllWithPartiallyMatchingInterfaces(
                    abiFunctionSignatures = setOf("function(string)"),
                    abiEventSignatures = setOf("Event(string)")
                )
            )
                .willReturn(
                    listOf(
                        InterfaceManifestJsonWithId(
                            id = InterfaceId("already-implemented"),
                            name = "Already Implemented",
                            description = "Already Implemented",
                            tags = emptySet(),
                            matchingEventDecorators = MANIFEST_JSON.eventDecorators,
                            matchingFunctionDecorators = emptyList()
                        ),
                        InterfaceManifestJsonWithId(
                            id = InterfaceId("not-yet-implemented"),
                            name = "Not Yet Implemented",
                            description = "Not Yet Implemented",
                            tags = emptySet(),
                            matchingEventDecorators = emptyList(),
                            matchingFunctionDecorators = MANIFEST_JSON.functionDecorators
                        )
                    )
                )
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = importedContractDecoratorRepository,
            contractInterfacesRepository = contractInterfacesRepository,
            contractMetadataRepository = mock()
        )

        verify("correct interfaces are suggested") {
            expectThat(service.getSuggestedInterfacesForImportedSmartContract(ID))
                .isEqualTo(
                    MatchingContractInterfaces(
                        listOf(
                            InterfaceManifestJsonWithId(
                                id = InterfaceId("not-yet-implemented"),
                                name = "Not Yet Implemented",
                                description = "Not Yet Implemented",
                                tags = emptySet(),
                                matchingEventDecorators = emptyList(),
                                matchingFunctionDecorators = MANIFEST_JSON.functionDecorators
                            )
                        ),
                        listOf(InterfaceId("not-yet-implemented"))
                    )
                )
        }
    }

    @Test
    fun mustCorrectlySuggestInterfacesByPriorityForImportedContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some imported contract deployment request will be returned") {
            call(contractDeploymentRequestRepository.getById(ID))
                .willReturn(CONTRACT_DEPLOYMENT_REQUEST)
        }

        val importedContractDecoratorRepository = mock<ImportedContractDecoratorRepository>()

        suppose("some imported contract manifest will be returned") {
            call(importedContractDecoratorRepository.getManifestJsonByContractIdAndProjectId(CONTRACT_ID, PROJECT_ID))
                .willReturn(MULTI_FUNCTION_MANIFEST_JSON)
        }

        val contractInterfacesRepository = mock<ContractInterfacesRepository>()

        suppose("some partially matching contract interfaces will be returned") {
            call(
                contractInterfacesRepository.getAllWithPartiallyMatchingInterfaces(
                    abiFunctionSignatures = setOf("function(string)", "another(address)", "test(uint,int)"),
                    abiEventSignatures = setOf("Event(string)")
                )
            )
                .willReturn(
                    listOf(
                        InterfaceManifestJsonWithId(
                            id = InterfaceId("already-implemented"),
                            name = "Already Implemented",
                            description = "Already Implemented",
                            tags = emptySet(),
                            matchingEventDecorators = MULTI_FUNCTION_MANIFEST_JSON.eventDecorators,
                            matchingFunctionDecorators = emptyList()
                        ),
                        InterfaceManifestJsonWithId(
                            id = InterfaceId("best-matching"),
                            name = "Best matching",
                            description = "Best matching",
                            tags = emptySet(),
                            matchingEventDecorators = MULTI_FUNCTION_MANIFEST_JSON.eventDecorators,
                            matchingFunctionDecorators = MULTI_FUNCTION_MANIFEST_JSON.functionDecorators.filterNot {
                                it.name == "test"
                            }
                        ),
                        InterfaceManifestJsonWithId(
                            id = InterfaceId("second-best-matching-with-overlap"),
                            name = "Second-best matching with overlap",
                            description = "Second-best matching with overlap",
                            tags = emptySet(),
                            matchingEventDecorators = emptyList(),
                            matchingFunctionDecorators = MULTI_FUNCTION_MANIFEST_JSON.functionDecorators.filterNot {
                                it.name == "another"
                            }
                        ),
                        InterfaceManifestJsonWithId(
                            id = InterfaceId("empty"),
                            name = "Empty",
                            description = "Empty",
                            tags = emptySet(),
                            matchingEventDecorators = emptyList(),
                            matchingFunctionDecorators = emptyList()
                        ),
                        InterfaceManifestJsonWithId(
                            id = InterfaceId("least-matching"),
                            name = "Least matching",
                            description = "Least matching",
                            tags = emptySet(),
                            matchingEventDecorators = emptyList(),
                            matchingFunctionDecorators = MULTI_FUNCTION_MANIFEST_JSON.functionDecorators.filter {
                                it.name == "test"
                            }
                        )
                    )
                )
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = importedContractDecoratorRepository,
            contractInterfacesRepository = contractInterfacesRepository,
            contractMetadataRepository = mock()
        )

        verify("correct interfaces are suggested") {
            expectThat(service.getSuggestedInterfacesForImportedSmartContract(ID))
                .isEqualTo(
                    MatchingContractInterfaces(
                        listOf(
                            InterfaceManifestJsonWithId(
                                id = InterfaceId("best-matching"),
                                name = "Best matching",
                                description = "Best matching",
                                tags = emptySet(),
                                matchingEventDecorators = MULTI_FUNCTION_MANIFEST_JSON.eventDecorators,
                                matchingFunctionDecorators = MULTI_FUNCTION_MANIFEST_JSON.functionDecorators.filterNot {
                                    it.name == "test"
                                }
                            ),
                            InterfaceManifestJsonWithId(
                                id = InterfaceId("second-best-matching-with-overlap"),
                                name = "Second-best matching with overlap",
                                description = "Second-best matching with overlap",
                                tags = emptySet(),
                                matchingEventDecorators = emptyList(),
                                matchingFunctionDecorators = MULTI_FUNCTION_MANIFEST_JSON.functionDecorators.filterNot {
                                    it.name == "another"
                                }
                            ),
                            InterfaceManifestJsonWithId(
                                id = InterfaceId("least-matching"),
                                name = "Least matching",
                                description = "Least matching",
                                tags = emptySet(),
                                matchingEventDecorators = emptyList(),
                                matchingFunctionDecorators = MULTI_FUNCTION_MANIFEST_JSON.functionDecorators.filter {
                                    it.name == "test"
                                }
                            ),
                            InterfaceManifestJsonWithId(
                                id = InterfaceId("empty"),
                                name = "Empty",
                                description = "Empty",
                                tags = emptySet(),
                                matchingEventDecorators = emptyList(),
                                matchingFunctionDecorators = emptyList()
                            )
                        ),
                        listOf(
                            InterfaceId("best-matching"),
                            InterfaceId("least-matching")
                        )
                    )
                )
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenSuggestingInterfacesForNonExistentContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("null will be returned for contract deployment request") {
            call(contractDeploymentRequestRepository.getById(ID))
                .willReturn(null)
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = mock(),
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.getSuggestedInterfacesForImportedSmartContract(ID)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenSuggestingInterfacesForNonImportedContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some non-imported contract deployment request will be returned") {
            call(contractDeploymentRequestRepository.getById(ID))
                .willReturn(CONTRACT_DEPLOYMENT_REQUEST.copy(imported = false))
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = mock(),
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.getSuggestedInterfacesForImportedSmartContract(ID)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenSuggestingInterfacesForNonExistentImportedContractManifest() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some imported contract deployment request will be returned") {
            call(contractDeploymentRequestRepository.getById(ID))
                .willReturn(CONTRACT_DEPLOYMENT_REQUEST)
        }

        val importedContractDecoratorRepository = mock<ImportedContractDecoratorRepository>()

        suppose("null will be returned for imported contract manifest") {
            call(importedContractDecoratorRepository.getManifestJsonByContractIdAndProjectId(CONTRACT_ID, PROJECT_ID))
                .willReturn(null)
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = importedContractDecoratorRepository,
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.getSuggestedInterfacesForImportedSmartContract(ID)
            }
        }
    }

    @Test
    fun mustCorrectlyAddContractInterfacesToImportedContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some imported contract deployment request will be returned") {
            call(contractDeploymentRequestRepository.getById(ID))
                .willReturn(CONTRACT_DEPLOYMENT_REQUEST)
        }

        val importedContractDecoratorRepository = mock<ImportedContractDecoratorRepository>()

        suppose("some imported contract manifest will be returned") {
            call(importedContractDecoratorRepository.getManifestJsonByContractIdAndProjectId(CONTRACT_ID, PROJECT_ID))
                .willReturn(MANIFEST_JSON)
        }

        suppose("some imported contract artifact will be returned") {
            call(importedContractDecoratorRepository.getArtifactJsonByContractIdAndProjectId(CONTRACT_ID, PROJECT_ID))
                .willReturn(ARTIFACT_JSON)
        }

        val contractInterfacesRepository = mock<ContractInterfacesRepository>()

        suppose("some interfaces are in the contract interfaces repository") {
            call(contractInterfacesRepository.getById(anyValueClass(InterfaceId(""))))
                .willReturn(EMPTY_CONTRACT_INTERFACE)
        }

        val contractMetadataRepository = mock<ContractMetadataRepository>()

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = importedContractDecoratorRepository,
            contractInterfacesRepository = contractInterfacesRepository,
            contractMetadataRepository = contractMetadataRepository
        )

        val newInterface = listOf(InterfaceId("new-interface"))
        val newInterfaces = MANIFEST_JSON.implements + newInterface.map { it.value }

        suppose("interface is added") {
            service.addInterfacesToImportedContract(ID, PROJECT_ID, newInterface)
        }

        verify("interface is correctly added") {
            expectInteractions(importedContractDecoratorRepository) {
                once.getManifestJsonByContractIdAndProjectId(CONTRACT_ID, PROJECT_ID)
                once.getArtifactJsonByContractIdAndProjectId(CONTRACT_ID, PROJECT_ID)
                once.updateInterfaces(
                    contractId = CONTRACT_ID,
                    projectId = PROJECT_ID,
                    interfaces = newInterfaces.map { InterfaceId(it) }.toList(),
                    manifest = MANIFEST_JSON.copy(implements = newInterfaces)
                )
            }

            expectInteractions(contractMetadataRepository) {
                once.updateInterfaces(
                    contractId = CONTRACT_ID,
                    projectId = PROJECT_ID,
                    interfaces = newInterfaces.map { InterfaceId(it) }.toList()
                )
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenAddingInterfacesToNonExistentContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("null will be returned for contract deployment request") {
            call(contractDeploymentRequestRepository.getById(ID))
                .willReturn(null)
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = mock(),
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.addInterfacesToImportedContract(ID, PROJECT_ID, listOf(InterfaceId("new-interface")))
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenAddingInterfacesToNonImportedContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some non-imported contract deployment request will be returned") {
            call(contractDeploymentRequestRepository.getById(ID))
                .willReturn(CONTRACT_DEPLOYMENT_REQUEST.copy(imported = false))
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = mock(),
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.addInterfacesToImportedContract(ID, PROJECT_ID, listOf(InterfaceId("new-interface")))
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenAddingInterfacesToNonOwnedImportedContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some non-owned contract deployment request will be returned") {
            call(contractDeploymentRequestRepository.getById(ID))
                .willReturn(CONTRACT_DEPLOYMENT_REQUEST.copy(projectId = ProjectId(UUID.randomUUID())))
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = mock(),
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.addInterfacesToImportedContract(ID, PROJECT_ID, listOf(InterfaceId("new-interface")))
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenAddingInterfacesToNonExistentImportedContractManifest() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some imported contract deployment request will be returned") {
            call(contractDeploymentRequestRepository.getById(ID))
                .willReturn(CONTRACT_DEPLOYMENT_REQUEST)
        }

        val importedContractDecoratorRepository = mock<ImportedContractDecoratorRepository>()

        suppose("null will be returned for imported contract manifest") {
            call(importedContractDecoratorRepository.getManifestJsonByContractIdAndProjectId(CONTRACT_ID, PROJECT_ID))
                .willReturn(null)
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = importedContractDecoratorRepository,
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.addInterfacesToImportedContract(ID, PROJECT_ID, listOf(InterfaceId("new-interface")))
            }
        }
    }

    @Test
    fun mustThrowContractInterfaceNotFoundExceptionWhenAddingNonExistentContractInterfaces() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some imported contract deployment request will be returned") {
            call(contractDeploymentRequestRepository.getById(ID))
                .willReturn(CONTRACT_DEPLOYMENT_REQUEST)
        }

        val importedContractDecoratorRepository = mock<ImportedContractDecoratorRepository>()

        suppose("some imported contract manifest will be returned") {
            call(importedContractDecoratorRepository.getManifestJsonByContractIdAndProjectId(CONTRACT_ID, PROJECT_ID))
                .willReturn(MANIFEST_JSON)
        }

        suppose("some imported contract artifact will be returned") {
            call(importedContractDecoratorRepository.getArtifactJsonByContractIdAndProjectId(CONTRACT_ID, PROJECT_ID))
                .willReturn(ARTIFACT_JSON)
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = importedContractDecoratorRepository,
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ContractInterfaceNotFoundException is thrown") {
            expectThrows<ContractInterfaceNotFoundException> {
                service.addInterfacesToImportedContract(ID, PROJECT_ID, listOf(InterfaceId("new-interface")))
            }
        }
    }

    @Test
    fun mustCorrectlyRemoveContractInterfacesFromImportedContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some imported contract deployment request will be returned") {
            call(contractDeploymentRequestRepository.getById(ID))
                .willReturn(CONTRACT_DEPLOYMENT_REQUEST)
        }

        val importedContractDecoratorRepository = mock<ImportedContractDecoratorRepository>()

        suppose("some imported contract manifest will be returned") {
            call(importedContractDecoratorRepository.getManifestJsonByContractIdAndProjectId(CONTRACT_ID, PROJECT_ID))
                .willReturn(MANIFEST_JSON)
        }

        suppose("some imported contract artifact will be returned") {
            call(importedContractDecoratorRepository.getArtifactJsonByContractIdAndProjectId(CONTRACT_ID, PROJECT_ID))
                .willReturn(ARTIFACT_JSON)
        }

        val contractInterfacesRepository = mock<ContractInterfacesRepository>()

        suppose("some interfaces are in the contract interfaces repository") {
            call(contractInterfacesRepository.getById(anyValueClass(InterfaceId(""))))
                .willReturn(EMPTY_CONTRACT_INTERFACE)
        }

        val contractMetadataRepository = mock<ContractMetadataRepository>()

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = importedContractDecoratorRepository,
            contractInterfacesRepository = contractInterfacesRepository,
            contractMetadataRepository = contractMetadataRepository
        )

        suppose("interface is removed") {
            service.removeInterfacesFromImportedContract(
                importedContractId = ID,
                projectId = PROJECT_ID,
                interfaces = MANIFEST_JSON.implements.map { InterfaceId(it) }
            )
        }

        verify("interface is correctly removed") {
            expectInteractions(importedContractDecoratorRepository) {
                once.getManifestJsonByContractIdAndProjectId(CONTRACT_ID, PROJECT_ID)
                once.getArtifactJsonByContractIdAndProjectId(CONTRACT_ID, PROJECT_ID)
                once.updateInterfaces(
                    contractId = CONTRACT_ID,
                    projectId = PROJECT_ID,
                    interfaces = emptyList(),
                    manifest = MANIFEST_JSON.copy(implements = emptySet())
                )
            }

            expectInteractions(contractMetadataRepository) {
                once.updateInterfaces(
                    contractId = CONTRACT_ID,
                    projectId = PROJECT_ID,
                    interfaces = emptyList()
                )
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenRemovingInterfacesFromNonExistentContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("null will be returned for contract deployment request") {
            call(contractDeploymentRequestRepository.getById(ID))
                .willReturn(null)
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = mock(),
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.removeInterfacesFromImportedContract(ID, PROJECT_ID, listOf(InterfaceId("new-interface")))
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenRemovingInterfacesFromNonImportedContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some non-imported contract deployment request will be returned") {
            call(contractDeploymentRequestRepository.getById(ID))
                .willReturn(CONTRACT_DEPLOYMENT_REQUEST.copy(imported = false))
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = mock(),
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.removeInterfacesFromImportedContract(ID, PROJECT_ID, listOf(InterfaceId("new-interface")))
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenRemovingInterfacesFromNonOwnedImportedContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some non-owned contract deployment request will be returned") {
            call(contractDeploymentRequestRepository.getById(ID))
                .willReturn(CONTRACT_DEPLOYMENT_REQUEST.copy(projectId = ProjectId(UUID.randomUUID())))
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = mock(),
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.removeInterfacesFromImportedContract(ID, PROJECT_ID, listOf(InterfaceId("new-interface")))
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenRemovingInterfacesFromNonExistentImportedContractManifest() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some imported contract deployment request will be returned") {
            call(contractDeploymentRequestRepository.getById(ID))
                .willReturn(CONTRACT_DEPLOYMENT_REQUEST)
        }

        val importedContractDecoratorRepository = mock<ImportedContractDecoratorRepository>()

        suppose("null will be returned for imported contract manifest") {
            call(importedContractDecoratorRepository.getManifestJsonByContractIdAndProjectId(CONTRACT_ID, PROJECT_ID))
                .willReturn(null)
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = importedContractDecoratorRepository,
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.removeInterfacesFromImportedContract(ID, PROJECT_ID, listOf(InterfaceId("new-interface")))
            }
        }
    }

    @Test
    fun mustCorrectlySetContractInterfacesForImportedContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some imported contract deployment request will be returned") {
            call(contractDeploymentRequestRepository.getById(ID))
                .willReturn(CONTRACT_DEPLOYMENT_REQUEST)
        }

        val importedContractDecoratorRepository = mock<ImportedContractDecoratorRepository>()

        suppose("some imported contract manifest will be returned") {
            call(importedContractDecoratorRepository.getManifestJsonByContractIdAndProjectId(CONTRACT_ID, PROJECT_ID))
                .willReturn(MANIFEST_JSON)
        }

        suppose("some imported contract artifact will be returned") {
            call(importedContractDecoratorRepository.getArtifactJsonByContractIdAndProjectId(CONTRACT_ID, PROJECT_ID))
                .willReturn(ARTIFACT_JSON)
        }

        val contractInterfacesRepository = mock<ContractInterfacesRepository>()

        suppose("some interfaces are in the contract interfaces repository") {
            call(contractInterfacesRepository.getById(anyValueClass(InterfaceId(""))))
                .willReturn(EMPTY_CONTRACT_INTERFACE)
        }

        val contractMetadataRepository = mock<ContractMetadataRepository>()

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = importedContractDecoratorRepository,
            contractInterfacesRepository = contractInterfacesRepository,
            contractMetadataRepository = contractMetadataRepository
        )

        val newInterfaces = listOf(InterfaceId("new-interface"))

        suppose("interfaces are set") {
            service.setImportedContractInterfaces(ID, PROJECT_ID, newInterfaces)
        }

        verify("interfaces are correctly set") {
            expectInteractions(importedContractDecoratorRepository) {
                once.getManifestJsonByContractIdAndProjectId(CONTRACT_ID, PROJECT_ID)
                once.getArtifactJsonByContractIdAndProjectId(CONTRACT_ID, PROJECT_ID)
                once.updateInterfaces(
                    contractId = CONTRACT_ID,
                    projectId = PROJECT_ID,
                    interfaces = newInterfaces,
                    manifest = MANIFEST_JSON.copy(implements = newInterfaces.map { it.value }.toSet())
                )
            }

            expectInteractions(contractMetadataRepository) {
                once.updateInterfaces(
                    contractId = CONTRACT_ID,
                    projectId = PROJECT_ID,
                    interfaces = newInterfaces
                )
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenSettingInterfacesForNonExistentContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("null will be returned for contract deployment request") {
            call(contractDeploymentRequestRepository.getById(ID))
                .willReturn(null)
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = mock(),
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.setImportedContractInterfaces(ID, PROJECT_ID, listOf(InterfaceId("new-interface")))
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenSettingInterfacesForNonImportedContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some non-imported contract deployment request will be returned") {
            call(contractDeploymentRequestRepository.getById(ID))
                .willReturn(CONTRACT_DEPLOYMENT_REQUEST.copy(imported = false))
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = mock(),
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.setImportedContractInterfaces(ID, PROJECT_ID, listOf(InterfaceId("new-interface")))
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenSettingInterfacesForNonOwnedImportedContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some non-owned contract deployment request will be returned") {
            call(contractDeploymentRequestRepository.getById(ID))
                .willReturn(CONTRACT_DEPLOYMENT_REQUEST.copy(projectId = ProjectId(UUID.randomUUID())))
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = mock(),
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.setImportedContractInterfaces(ID, PROJECT_ID, listOf(InterfaceId("new-interface")))
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenSettingInterfacesForNonExistentImportedContractManifest() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some imported contract deployment request will be returned") {
            call(contractDeploymentRequestRepository.getById(ID))
                .willReturn(CONTRACT_DEPLOYMENT_REQUEST)
        }

        val importedContractDecoratorRepository = mock<ImportedContractDecoratorRepository>()

        suppose("null will be returned for imported contract manifest") {
            call(importedContractDecoratorRepository.getManifestJsonByContractIdAndProjectId(CONTRACT_ID, PROJECT_ID))
                .willReturn(null)
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = importedContractDecoratorRepository,
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.setImportedContractInterfaces(ID, PROJECT_ID, listOf(InterfaceId("new-interface")))
            }
        }
    }

    @Test
    fun mustThrowContractInterfaceNotFoundExceptionWhenSettingNonExistentContractInterfaces() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some imported contract deployment request will be returned") {
            call(contractDeploymentRequestRepository.getById(ID))
                .willReturn(CONTRACT_DEPLOYMENT_REQUEST)
        }

        val importedContractDecoratorRepository = mock<ImportedContractDecoratorRepository>()

        suppose("some imported contract manifest will be returned") {
            call(importedContractDecoratorRepository.getManifestJsonByContractIdAndProjectId(CONTRACT_ID, PROJECT_ID))
                .willReturn(MANIFEST_JSON)
        }

        suppose("some imported contract artifact will be returned") {
            call(importedContractDecoratorRepository.getArtifactJsonByContractIdAndProjectId(CONTRACT_ID, PROJECT_ID))
                .willReturn(ARTIFACT_JSON)
        }

        val service = ContractInterfacesServiceImpl(
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            importedContractDecoratorRepository = importedContractDecoratorRepository,
            contractInterfacesRepository = mock(),
            contractMetadataRepository = mock()
        )

        verify("ContractInterfaceNotFoundException is thrown") {
            expectThrows<ContractInterfaceNotFoundException> {
                service.setImportedContractInterfaces(ID, PROJECT_ID, listOf(InterfaceId("new-interface")))
            }
        }
    }
}
