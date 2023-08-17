package polycode.service

import com.fasterxml.jackson.databind.JsonNode
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import polycode.TestBase
import polycode.TestData
import polycode.blockchain.BlockchainService
import polycode.blockchain.properties.ChainSpec
import polycode.config.JsonConfig
import polycode.exception.ContractDecoratorBinaryMismatchException
import polycode.exception.ContractNotFoundException
import polycode.exception.ResourceNotFoundException
import polycode.features.api.access.model.result.Project
import polycode.features.contract.abi.model.AddressType
import polycode.features.contract.abi.model.Tuple
import polycode.features.contract.abi.service.EthereumAbiDecoderService
import polycode.features.contract.deployment.model.json.AbiInputOutput
import polycode.features.contract.deployment.model.json.AbiObject
import polycode.features.contract.deployment.model.json.ArtifactJson
import polycode.features.contract.deployment.model.json.ConstructorDecorator
import polycode.features.contract.deployment.model.json.FunctionDecorator
import polycode.features.contract.deployment.model.json.ManifestJson
import polycode.features.contract.deployment.model.json.TypeDecorator
import polycode.features.contract.deployment.model.params.StoreContractDeploymentRequestParams
import polycode.features.contract.deployment.model.result.ContractDecorator
import polycode.features.contract.deployment.model.result.ContractDeploymentRequest
import polycode.features.contract.deployment.model.result.ContractParameter
import polycode.features.contract.deployment.repository.ContractDecoratorRepository
import polycode.features.contract.deployment.repository.ContractDeploymentRequestRepository
import polycode.features.contract.deployment.repository.ContractMetadataRepository
import polycode.features.contract.deployment.repository.ImportedContractDecoratorRepository
import polycode.features.contract.importing.model.json.DecompiledContractJson
import polycode.features.contract.importing.model.params.ImportContractParams
import polycode.features.contract.importing.service.ContractDecompilerService
import polycode.features.contract.importing.service.ContractImportServiceImpl
import polycode.features.contract.importing.service.ContractImportServiceImpl.Companion.TypeAndValue
import polycode.features.contract.readcall.model.params.ExecuteReadonlyFunctionCallParams
import polycode.features.contract.readcall.model.params.OutputParameter
import polycode.features.contract.readcall.model.result.ReadonlyFunctionCallResult
import polycode.features.functions.encoding.model.FunctionArgument
import polycode.features.functions.encoding.service.EthereumFunctionEncoderService
import polycode.features.functions.encoding.service.FunctionEncoderService
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.generated.jooq.id.ContractMetadataId
import polycode.generated.jooq.id.ImportedContractDecoratorId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.model.DeserializableEvent
import polycode.model.ScreenConfig
import polycode.model.result.ContractBinaryInfo
import polycode.model.result.ContractMetadata
import polycode.model.result.FullContractDeploymentTransactionInfo
import polycode.util.Balance
import polycode.util.BaseUrl
import polycode.util.BlockNumber
import polycode.util.ChainId
import polycode.util.Constants
import polycode.util.ContractAddress
import polycode.util.ContractBinaryData
import polycode.util.ContractId
import polycode.util.EthStorageSlot
import polycode.util.FunctionData
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import polycode.util.ZeroAddress
import java.math.BigInteger
import java.util.UUID
import kotlin.time.Duration.Companion.days

class ContractImportServiceTest : TestBase() {

    companion object {
        private val PROXY_IMPLEMENTATION_SLOT =
            EthStorageSlot("0x360894a13ba1a3210667c828492db98dca3e2076cc3735a920a3ca505d382bbc")
        private val OPENZEPPELIN_SLOT =
            EthStorageSlot("0x7050c9e0f4ca769c69bd3a8ef740bc37934f8e2c036e5a723fd8ee048ed3f8c3")
        private val PROXY_BEACON_SLOT =
            EthStorageSlot("0xa3f0ad74e5423aebfd80d3ef4346578335a9a72aeaee59ff6cb3582b35133d50")
        private val objectMapper = JsonConfig().objectMapper()
        private val CHAIN_ID = ChainId(1337L)
        private val PROJECT = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = UserId(UUID.randomUUID()),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        private val OTHER_PROJECT = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = UserId(UUID.randomUUID()),
            baseRedirectUrl = BaseUrl("other-redirect-url"),
            chainId = CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        private val CONTRACT_ADDRESS = ContractAddress("abc")
        private val CONTRACT_ID = ContractId("imported")
        private val PARAMS = ImportContractParams(
            alias = "alias",
            contractId = CONTRACT_ID,
            contractAddress = CONTRACT_ADDRESS,
            redirectUrl = null,
            arbitraryData = null,
            screenConfig = ScreenConfig.EMPTY
        )
        private val NON_IMPORTED_CONTRACT = ContractDeploymentRequest(
            id = ContractDeploymentRequestId(UUID.randomUUID()),
            alias = "non-imported-contract",
            name = "Non imported contract",
            description = "Non imported contract",
            contractId = ContractId("non-imported-contract"),
            constructorParams = TestData.EMPTY_JSON_ARRAY,
            contractData = ContractBinaryData("001122"),
            contractTags = emptyList(),
            contractImplements = emptyList(),
            initialEthAmount = Balance.ZERO,
            chainId = CHAIN_ID,
            redirectUrl = "redirect-url",
            projectId = PROJECT.id,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = objectMapper.valueToTree("{\"test\":true}"),
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            contractAddress = CONTRACT_ADDRESS,
            deployerAddress = WalletAddress("abc123def456"),
            txHash = TransactionHash("non-imported-tx-hash"),
            imported = false,
            proxy = false,
            implementationContractAddress = null
        )
        private val IMPORTED_CONTRACT = ContractDeploymentRequest(
            id = ContractDeploymentRequestId(UUID.randomUUID()),
            alias = "imported-contract",
            name = "Imported contract",
            description = "Imported contract",
            contractId = ContractId("imported-contract"),
            constructorParams = TestData.EMPTY_JSON_ARRAY,
            contractData = ContractBinaryData("001122"),
            contractTags = emptyList(),
            contractImplements = emptyList(),
            initialEthAmount = Balance.ZERO,
            chainId = CHAIN_ID,
            redirectUrl = "redirect-url",
            projectId = PROJECT.id,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = objectMapper.valueToTree("{\"test\":true}"),
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            ),
            contractAddress = CONTRACT_ADDRESS,
            deployerAddress = WalletAddress("abc123def456"),
            txHash = TransactionHash("imported-tx-hash"),
            imported = true,
            proxy = false,
            implementationContractAddress = null
        )
        private val CONSTRUCTOR_PARAMS = listOf(FunctionArgument(WalletAddress("cafebabe")))
        private val ENCODED_CONSTRUCTOR_CALL = EthereumFunctionEncoderService().encodeConstructor(CONSTRUCTOR_PARAMS)
        private val CONSTRUCTOR_PARAMS_JSON = objectMapper.valueToTree<JsonNode>(
            listOf(TypeAndValue(type = "address", value = WalletAddress("cafebabe").rawValue))
        )
        private val CONSTRUCTOR_BYTES_32_JSON = objectMapper.valueToTree<JsonNode>(
            listOf(
                TypeAndValue(
                    type = "bytes32",
                    value = ENCODED_CONSTRUCTOR_CALL.withoutPrefix.chunked(2).map { it.toUByte(16).toByte() }
                )
            )
        )
        private const val CONSTRUCTOR_BYTECODE = "123456"
        private const val CONTRACT_BYTECODE = "abcdef1234567890abcdef"
        private val ARTIFACT_JSON = ArtifactJson(
            contractName = "imported",
            sourceName = "imported.sol",
            abi = listOf(
                AbiObject(
                    anonymous = false,
                    inputs = listOf(
                        AbiInputOutput(
                            components = null,
                            internalType = "address",
                            name = "someAddress",
                            type = "address",
                            indexed = false
                        )
                    ),
                    outputs = null,
                    stateMutability = null,
                    name = "",
                    type = "constructor"
                )
            ),
            bytecode = "$CONSTRUCTOR_BYTECODE$CONTRACT_BYTECODE",
            deployedBytecode = CONTRACT_BYTECODE,
            linkReferences = null,
            deployedLinkReferences = null
        )
        private val MANIFEST_JSON = ManifestJson(
            name = "imported",
            description = "imported",
            tags = emptySet(),
            implements = emptySet(),
            eventDecorators = emptyList(),
            constructorDecorators = listOf(
                ConstructorDecorator(
                    signature = "constructor(address)",
                    description = "",
                    parameterDecorators = listOf(
                        TypeDecorator(
                            name = "",
                            description = "",
                            recommendedTypes = emptyList(),
                            parameters = null,
                            hints = emptyList()
                        )
                    )
                )
            ),
            functionDecorators = emptyList()
        )
        private val CONTRACT_DECORATOR = ContractDecorator(
            id = CONTRACT_ID,
            artifact = ARTIFACT_JSON,
            manifest = MANIFEST_JSON,
            imported = true,
            interfacesProvider = null
        )
        private val CONTRACT_DEPLOYMENT_TRANSACTION_INFO = FullContractDeploymentTransactionInfo(
            hash = TransactionHash("tx-hash"),
            from = WalletAddress("123"),
            deployedContractAddress = PARAMS.contractAddress,
            data = FunctionData("${ARTIFACT_JSON.bytecode}${ENCODED_CONSTRUCTOR_CALL.withoutPrefix}"),
            value = Balance.ZERO,
            binary = ContractBinaryData(ARTIFACT_JSON.deployedBytecode),
            blockNumber = BlockNumber(BigInteger.ONE),
            events = emptyList()
        )
        private val CONTRACT_BINARY_INFO = ContractBinaryInfo(
            deployedContractAddress = PARAMS.contractAddress,
            binary = ContractBinaryData(ARTIFACT_JSON.deployedBytecode)
        )
        private const val INFO_MARKDOWN = "info-markdown"
        private const val PROXY_CONSTRUCTOR_BYTECODE = "123456abc"
        private const val PROXY_CONTRACT_BYTECODE = "abcdef000777"
        private val PROXY_ARTIFACT_JSON = ArtifactJson(
            contractName = "imported",
            sourceName = "imported.sol",
            abi = listOf(
                AbiObject(
                    anonymous = false,
                    inputs = listOf(
                        AbiInputOutput(
                            components = null,
                            internalType = "address",
                            name = "someAddress",
                            type = "address",
                            indexed = false
                        )
                    ),
                    outputs = null,
                    stateMutability = null,
                    name = "",
                    type = "constructor"
                ),
                AbiObject(
                    anonymous = false,
                    inputs = emptyList(),
                    outputs = emptyList(),
                    stateMutability = null,
                    name = "implementation",
                    type = "function"
                )
            ),
            bytecode = "$PROXY_CONSTRUCTOR_BYTECODE$PROXY_CONTRACT_BYTECODE",
            deployedBytecode = PROXY_CONTRACT_BYTECODE,
            linkReferences = null,
            deployedLinkReferences = null
        )
        private val PROXY_MANIFEST_JSON = ManifestJson(
            name = "imported",
            description = "imported",
            tags = emptySet(),
            implements = emptySet(),
            eventDecorators = emptyList(),
            constructorDecorators = listOf(
                ConstructorDecorator(
                    signature = "constructor(address)",
                    description = "",
                    parameterDecorators = listOf(
                        TypeDecorator(
                            name = "",
                            description = "",
                            recommendedTypes = emptyList(),
                            parameters = null,
                            hints = emptyList()
                        )
                    )
                )
            ),
            functionDecorators = listOf(
                FunctionDecorator(
                    signature = "implementation()",
                    name = "implementation",
                    description = "",
                    parameterDecorators = emptyList(),
                    returnDecorators = emptyList(),
                    emittableEvents = emptyList(),
                    readOnly = false
                )
            )
        )
        private val PROXY_CONTRACT_DEPLOYMENT_TRANSACTION_INFO = FullContractDeploymentTransactionInfo(
            hash = TransactionHash("proxy-tx-hash"),
            from = WalletAddress("123"),
            deployedContractAddress = ContractAddress("cafe000bafe000abc123"),
            data = FunctionData("${PROXY_ARTIFACT_JSON.bytecode}${ENCODED_CONSTRUCTOR_CALL.withoutPrefix}"),
            value = Balance.ZERO,
            binary = ContractBinaryData(PROXY_ARTIFACT_JSON.deployedBytecode),
            blockNumber = BlockNumber(BigInteger.ONE),
            events = emptyList()
        )
        private val PROXY_CONTRACT_DECORATOR = ContractDecorator(
            id = CONTRACT_ID,
            artifact = PROXY_ARTIFACT_JSON,
            manifest = PROXY_MANIFEST_JSON,
            imported = true,
            interfacesProvider = null
        )
        private val EVENTS = listOf<DeserializableEvent>()
    }

    @Test
    fun mustCorrectlyImportExistingNonImportedContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some non-imported contract will be returned from contract deployment request repository") {
            call(contractDeploymentRequestRepository.getByContractAddressAndChainId(CONTRACT_ADDRESS, CHAIN_ID))
                .willReturn(NON_IMPORTED_CONTRACT)
        }

        val id = ContractDeploymentRequestId(UUID.randomUUID())
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            call(uuidProvider.getUuid(ContractDeploymentRequestId))
                .willReturn(id)
        }

        val timestamp = TestData.TIMESTAMP + 1.days
        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some UTC date-time will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(timestamp)
        }

        val storeParams = StoreContractDeploymentRequestParams(
            id = id,
            alias = PARAMS.alias,
            contractId = NON_IMPORTED_CONTRACT.contractId,
            contractData = NON_IMPORTED_CONTRACT.contractData,
            constructorParams = NON_IMPORTED_CONTRACT.constructorParams,
            deployerAddress = NON_IMPORTED_CONTRACT.deployerAddress,
            initialEthAmount = NON_IMPORTED_CONTRACT.initialEthAmount,
            chainId = CHAIN_ID,
            redirectUrl = "${OTHER_PROJECT.baseRedirectUrl.value}/request-deploy/${id.value}/action",
            projectId = OTHER_PROJECT.id,
            createdAt = timestamp,
            arbitraryData = PARAMS.arbitraryData,
            screenConfig = PARAMS.screenConfig,
            imported = false,
            proxy = false,
            implementationContractAddress = null
        )

        suppose("request will be correctly copied") {
            call(contractDeploymentRequestRepository.store(storeParams, Constants.NIL_PROJECT_ID))
                .willReturn(NON_IMPORTED_CONTRACT.copy(id = id))
        }

        val service = ContractImportServiceImpl(
            abiDecoderService = EthereumAbiDecoderService(),
            contractDecompilerService = mock(),
            abiProviderService = mock(),
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = mock(),
            blockchainService = mock(),
            uuidProvider = uuidProvider,
            utcDateTimeProvider = utcDateTimeProvider,
            objectMapper = objectMapper
        )

        verify("contract is correctly imported") {
            expectThat(service.importExistingContract(PARAMS, OTHER_PROJECT))
                .isEqualTo(id)

            expectInteractions(contractDeploymentRequestRepository) {
                once.getByContractAddressAndChainId(CONTRACT_ADDRESS, CHAIN_ID)
                once.store(
                    params = storeParams,
                    metadataProjectId = Constants.NIL_PROJECT_ID
                )
                once.setContractAddress(id, NON_IMPORTED_CONTRACT.contractAddress!!)
                once.setTxInfo(id, NON_IMPORTED_CONTRACT.txHash!!, NON_IMPORTED_CONTRACT.deployerAddress!!)
            }
        }
    }

    @Test
    fun mustCorrectlyImportExistingImportedContractFromAnotherProject() {
        val importedContractDecoratorRepository = mock<ImportedContractDecoratorRepository>()

        suppose("already imported contract manifest json, artifact json and info markdown will be returned") {
            call(
                importedContractDecoratorRepository.getManifestJsonByContractIdAndProjectId(
                    contractId = IMPORTED_CONTRACT.contractId,
                    projectId = PROJECT.id
                )
            )
                .willReturn(MANIFEST_JSON)
            call(
                importedContractDecoratorRepository.getArtifactJsonByContractIdAndProjectId(
                    contractId = IMPORTED_CONTRACT.contractId,
                    projectId = PROJECT.id
                )
            )
                .willReturn(ARTIFACT_JSON)
            call(
                importedContractDecoratorRepository.getInfoMarkdownByContractIdAndProjectId(
                    contractId = IMPORTED_CONTRACT.contractId,
                    projectId = PROJECT.id
                )
            )
                .willReturn(INFO_MARKDOWN)
        }

        val newContractId = ContractId("imported-${CONTRACT_ADDRESS.rawValue}-${CHAIN_ID.value}")

        suppose("contract decorator is not imported to other project") {
            call(
                importedContractDecoratorRepository.getByContractIdAndProjectId(
                    contractId = newContractId,
                    projectId = OTHER_PROJECT.id
                )
            )
                .willReturn(null)
        }

        val newDecoratorId = ImportedContractDecoratorId(UUID.randomUUID())
        val contractMetadataId = ContractMetadataId(UUID.randomUUID())
        val newlyImportedContractId = ContractDeploymentRequestId(UUID.randomUUID())
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            call(uuidProvider.getUuid(ImportedContractDecoratorId))
                .willReturn(newDecoratorId)
            call(uuidProvider.getUuid(ContractMetadataId))
                .willReturn(contractMetadataId)
            call(uuidProvider.getUuid(ContractDeploymentRequestId))
                .willReturn(newlyImportedContractId)
        }

        val newDecoratorTimestamp = TestData.TIMESTAMP + 1.days
        val newlyImportedContractTimestamp = TestData.TIMESTAMP + 2.days
        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some UTC date-time will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(newDecoratorTimestamp, newlyImportedContractTimestamp)
        }

        val newDecorator = ContractDecorator(
            id = newContractId,
            artifact = ARTIFACT_JSON,
            manifest = MANIFEST_JSON,
            imported = true,
            interfacesProvider = null
        )

        suppose("contract decorator is stored for other project") {
            call(
                importedContractDecoratorRepository.store(
                    id = newDecoratorId,
                    projectId = OTHER_PROJECT.id,
                    contractId = newContractId,
                    manifestJson = MANIFEST_JSON,
                    artifactJson = ARTIFACT_JSON,
                    infoMarkdown = INFO_MARKDOWN,
                    importedAt = newDecoratorTimestamp,
                    previewOnly = false
                )
            ).willReturn(newDecorator)
        }

        val storeParams = StoreContractDeploymentRequestParams(
            id = newlyImportedContractId,
            alias = PARAMS.alias,
            contractId = newContractId,
            contractData = IMPORTED_CONTRACT.contractData,
            constructorParams = IMPORTED_CONTRACT.constructorParams,
            deployerAddress = IMPORTED_CONTRACT.deployerAddress,
            initialEthAmount = IMPORTED_CONTRACT.initialEthAmount,
            chainId = CHAIN_ID,
            redirectUrl = "${OTHER_PROJECT.baseRedirectUrl.value}/request-deploy/${newlyImportedContractId.value}" +
                "/action",
            projectId = OTHER_PROJECT.id,
            createdAt = newlyImportedContractTimestamp,
            arbitraryData = PARAMS.arbitraryData,
            screenConfig = PARAMS.screenConfig,
            imported = true,
            proxy = false,
            implementationContractAddress = null
        )

        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some imported contract will be returned from contract deployment request repository") {
            call(contractDeploymentRequestRepository.getByContractAddressAndChainId(CONTRACT_ADDRESS, CHAIN_ID))
                .willReturn(IMPORTED_CONTRACT)
        }

        suppose("request will be correctly copied") {
            call(contractDeploymentRequestRepository.store(storeParams, OTHER_PROJECT.id))
                .willReturn(IMPORTED_CONTRACT.copy(id = newlyImportedContractId))
        }

        val contractMetadataRepository = mock<ContractMetadataRepository>()

        val service = ContractImportServiceImpl(
            abiDecoderService = EthereumAbiDecoderService(),
            contractDecompilerService = mock(),
            abiProviderService = mock(),
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepository,
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = importedContractDecoratorRepository,
            blockchainService = mock(),
            uuidProvider = uuidProvider,
            utcDateTimeProvider = utcDateTimeProvider,
            objectMapper = objectMapper
        )

        verify("contract is correctly imported") {
            expectThat(service.importExistingContract(PARAMS, OTHER_PROJECT))
                .isEqualTo(newlyImportedContractId)

            expectInteractions(contractMetadataRepository) {
                once.createOrUpdate(
                    ContractMetadata(
                        id = contractMetadataId,
                        name = newDecorator.name,
                        description = newDecorator.description,
                        contractId = newContractId,
                        contractTags = newDecorator.tags,
                        contractImplements = newDecorator.implements,
                        projectId = OTHER_PROJECT.id
                    )
                )
            }

            expectInteractions(contractDeploymentRequestRepository) {
                once.getByContractAddressAndChainId(CONTRACT_ADDRESS, CHAIN_ID)
                once.store(
                    params = storeParams,
                    metadataProjectId = OTHER_PROJECT.id
                )
                once.setContractAddress(newlyImportedContractId, IMPORTED_CONTRACT.contractAddress!!)
                once.setTxInfo(newlyImportedContractId, IMPORTED_CONTRACT.txHash!!, IMPORTED_CONTRACT.deployerAddress!!)
            }
        }
    }

    @Test
    fun mustCorrectlyImportExistingImportedContractFromSameProject() {
        val importedContractDecoratorRepository = mock<ImportedContractDecoratorRepository>()

        suppose("already imported contract manifest json, artifact json and info markdown will be returned") {
            call(
                importedContractDecoratorRepository.getManifestJsonByContractIdAndProjectId(
                    contractId = IMPORTED_CONTRACT.contractId,
                    projectId = PROJECT.id
                )
            )
                .willReturn(MANIFEST_JSON)
            call(
                importedContractDecoratorRepository.getArtifactJsonByContractIdAndProjectId(
                    contractId = IMPORTED_CONTRACT.contractId,
                    projectId = PROJECT.id
                )
            )
                .willReturn(ARTIFACT_JSON)
            call(
                importedContractDecoratorRepository.getInfoMarkdownByContractIdAndProjectId(
                    contractId = IMPORTED_CONTRACT.contractId,
                    projectId = PROJECT.id
                )
            )
                .willReturn(INFO_MARKDOWN)
        }

        val newContractId = ContractId("imported-${CONTRACT_ADDRESS.rawValue}-${CHAIN_ID.value}")
        val decorator = ContractDecorator(
            id = newContractId,
            artifact = ARTIFACT_JSON,
            manifest = MANIFEST_JSON,
            imported = true,
            interfacesProvider = null
        )

        suppose("contract decorator is imported to same project") {
            call(
                importedContractDecoratorRepository.getByContractIdAndProjectId(
                    contractId = newContractId,
                    projectId = PROJECT.id
                )
            )
                .willReturn(decorator)
        }

        val contractMetadataId = ContractMetadataId(UUID.randomUUID())
        val newlyImportedContractId = ContractDeploymentRequestId(UUID.randomUUID())
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            call(uuidProvider.getUuid(ContractMetadataId))
                .willReturn(contractMetadataId)
            call(uuidProvider.getUuid(ContractDeploymentRequestId))
                .willReturn(newlyImportedContractId)
        }

        val newlyImportedContractTimestamp = TestData.TIMESTAMP + 2.days
        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some UTC date-time will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(newlyImportedContractTimestamp)
        }

        val storeParams = StoreContractDeploymentRequestParams(
            id = newlyImportedContractId,
            alias = PARAMS.alias,
            contractId = newContractId,
            contractData = IMPORTED_CONTRACT.contractData,
            constructorParams = IMPORTED_CONTRACT.constructorParams,
            deployerAddress = IMPORTED_CONTRACT.deployerAddress,
            initialEthAmount = IMPORTED_CONTRACT.initialEthAmount,
            chainId = CHAIN_ID,
            redirectUrl = "${PROJECT.baseRedirectUrl.value}/request-deploy/${newlyImportedContractId.value}/action",
            projectId = PROJECT.id,
            createdAt = newlyImportedContractTimestamp,
            arbitraryData = PARAMS.arbitraryData,
            screenConfig = PARAMS.screenConfig,
            imported = true,
            proxy = false,
            implementationContractAddress = null
        )

        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some imported contract will be returned from contract deployment request repository") {
            call(contractDeploymentRequestRepository.getByContractAddressAndChainId(CONTRACT_ADDRESS, CHAIN_ID))
                .willReturn(IMPORTED_CONTRACT)
        }

        suppose("request will be correctly copied") {
            call(contractDeploymentRequestRepository.store(storeParams, PROJECT.id))
                .willReturn(IMPORTED_CONTRACT.copy(id = newlyImportedContractId))
        }

        val contractMetadataRepository = mock<ContractMetadataRepository>()

        val service = ContractImportServiceImpl(
            abiDecoderService = EthereumAbiDecoderService(),
            contractDecompilerService = mock(),
            abiProviderService = mock(),
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepository,
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = importedContractDecoratorRepository,
            blockchainService = mock(),
            uuidProvider = uuidProvider,
            utcDateTimeProvider = utcDateTimeProvider,
            objectMapper = objectMapper
        )

        verify("contract is correctly imported") {
            expectThat(service.importExistingContract(PARAMS, PROJECT))
                .isEqualTo(newlyImportedContractId)

            expectInteractions(contractMetadataRepository) {
                once.createOrUpdate(
                    ContractMetadata(
                        id = contractMetadataId,
                        name = decorator.name,
                        description = decorator.description,
                        contractId = newContractId,
                        contractTags = decorator.tags,
                        contractImplements = decorator.implements,
                        projectId = PROJECT.id
                    )
                )
            }

            expectInteractions(contractDeploymentRequestRepository) {
                once.getByContractAddressAndChainId(CONTRACT_ADDRESS, CHAIN_ID)
                once.store(
                    params = storeParams,
                    metadataProjectId = PROJECT.id
                )
                once.setContractAddress(newlyImportedContractId, IMPORTED_CONTRACT.contractAddress!!)
                once.setTxInfo(newlyImportedContractId, IMPORTED_CONTRACT.txHash!!, IMPORTED_CONTRACT.deployerAddress!!)
            }
        }
    }

    @Test
    fun mustNotImportNonExistingContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("contract deployment request repository will return null") {
            call(contractDeploymentRequestRepository.getByContractAddressAndChainId(CONTRACT_ADDRESS, CHAIN_ID))
                .willReturn(null)
        }

        val service = ContractImportServiceImpl(
            abiDecoderService = EthereumAbiDecoderService(),
            contractDecompilerService = mock(),
            abiProviderService = mock(),
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = mock(),
            blockchainService = mock(),
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            objectMapper = objectMapper
        )

        verify("null is returned when importing non-existent contract") {
            expectThat(service.importExistingContract(PARAMS, PROJECT))
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyImportContractForSomeExistingContractDecorator() {
        val contractDecoratorRepository = mock<ContractDecoratorRepository>()

        suppose("contract decorator will be returned") {
            call(contractDecoratorRepository.getById(CONTRACT_ID))
                .willReturn(CONTRACT_DECORATOR)
        }

        val contractMetadataRepository = mock<ContractMetadataRepository>()

        suppose("contract metadata exists") {
            call(contractMetadataRepository.exists(CONTRACT_ID, Constants.NIL_PROJECT_ID))
                .willReturn(true)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(PROJECT.chainId, PROJECT.customRpcUrl)

        suppose("contract deployment transaction will be found on blockchain") {
            call(blockchainService.findContractDeploymentTransaction(chainSpec, PARAMS.contractAddress, EVENTS))
                .willReturn(CONTRACT_DEPLOYMENT_TRANSACTION_INFO)
        }

        val id = ContractDeploymentRequestId(UUID.randomUUID())
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            call(uuidProvider.getUuid(ContractDeploymentRequestId))
                .willReturn(id)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some UTC date-time will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        val service = ContractImportServiceImpl(
            abiDecoderService = EthereumAbiDecoderService(),
            contractDecompilerService = mock(),
            abiProviderService = mock(),
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepository,
            contractDecoratorRepository = contractDecoratorRepository,
            importedContractDecoratorRepository = mock(),
            blockchainService = blockchainService,
            uuidProvider = uuidProvider,
            utcDateTimeProvider = utcDateTimeProvider,
            objectMapper = objectMapper
        )

        verify("contract is successfully imported") {
            expectThat(service.importContract(PARAMS, PROJECT))
                .isEqualTo(id)

            expectInteractions(contractDeploymentRequestRepository) {
                once.store(
                    params = StoreContractDeploymentRequestParams(
                        id = id,
                        alias = PARAMS.alias,
                        contractId = CONTRACT_ID,
                        contractData = ContractBinaryData(CONTRACT_DEPLOYMENT_TRANSACTION_INFO.data.value),
                        constructorParams = CONSTRUCTOR_PARAMS_JSON,
                        deployerAddress = CONTRACT_DEPLOYMENT_TRANSACTION_INFO.from,
                        initialEthAmount = CONTRACT_DEPLOYMENT_TRANSACTION_INFO.value,
                        chainId = PROJECT.chainId,
                        redirectUrl = "${PROJECT.baseRedirectUrl.value}/request-deploy/${id.value}/action",
                        projectId = PROJECT.id,
                        createdAt = TestData.TIMESTAMP,
                        arbitraryData = PARAMS.arbitraryData,
                        screenConfig = PARAMS.screenConfig,
                        imported = true,
                        proxy = false,
                        implementationContractAddress = null
                    ),
                    metadataProjectId = Constants.NIL_PROJECT_ID
                )
                once.setContractAddress(id, PARAMS.contractAddress)
                once.setTxInfo(id, CONTRACT_DEPLOYMENT_TRANSACTION_INFO.hash, CONTRACT_DEPLOYMENT_TRANSACTION_INFO.from)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenImportingContractForNonExistentContractDecorator() {
        val contractDecoratorRepository = mock<ContractDecoratorRepository>()

        suppose("null will be returned for contract decorator") {
            call(contractDecoratorRepository.getById(CONTRACT_ID))
                .willReturn(null)
        }

        val service = ContractImportServiceImpl(
            abiDecoderService = EthereumAbiDecoderService(),
            contractDecompilerService = mock(),
            abiProviderService = mock(),
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = mock(),
            contractMetadataRepository = mock(),
            contractDecoratorRepository = contractDecoratorRepository,
            importedContractDecoratorRepository = mock(),
            blockchainService = mock(),
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            objectMapper = objectMapper
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.importContract(PARAMS, PROJECT)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenImportingContractForNonExistentContractMetadata() {
        val contractDecoratorRepository = mock<ContractDecoratorRepository>()

        suppose("contract decorator will be returned") {
            call(contractDecoratorRepository.getById(CONTRACT_ID))
                .willReturn(CONTRACT_DECORATOR)
        }

        val contractMetadataRepository = mock<ContractMetadataRepository>()

        suppose("contract metadata does not exist") {
            call(contractMetadataRepository.exists(CONTRACT_ID, Constants.NIL_PROJECT_ID))
                .willReturn(false)
        }

        val service = ContractImportServiceImpl(
            abiDecoderService = EthereumAbiDecoderService(),
            contractDecompilerService = mock(),
            abiProviderService = mock(),
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = mock(),
            contractMetadataRepository = contractMetadataRepository,
            contractDecoratorRepository = contractDecoratorRepository,
            importedContractDecoratorRepository = mock(),
            blockchainService = mock(),
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            objectMapper = objectMapper
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.importContract(PARAMS, PROJECT)
            }
        }
    }

    @Test
    fun mustThrowContractNotFoundExceptionWhenContractCannotBeFoundOnBlockchain() {
        val contractDecoratorRepository = mock<ContractDecoratorRepository>()

        suppose("contract decorator will be returned") {
            call(contractDecoratorRepository.getById(CONTRACT_ID))
                .willReturn(CONTRACT_DECORATOR)
        }

        val contractMetadataRepository = mock<ContractMetadataRepository>()

        suppose("contract metadata exists") {
            call(contractMetadataRepository.exists(CONTRACT_ID, Constants.NIL_PROJECT_ID))
                .willReturn(true)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(PROJECT.chainId, PROJECT.customRpcUrl)

        suppose("contract deployment transaction will not be found on blockchain") {
            call(blockchainService.findContractDeploymentTransaction(chainSpec, PARAMS.contractAddress, EVENTS))
                .willReturn(null)
        }

        val service = ContractImportServiceImpl(
            abiDecoderService = EthereumAbiDecoderService(),
            contractDecompilerService = mock(),
            abiProviderService = mock(),
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = mock(),
            contractMetadataRepository = contractMetadataRepository,
            contractDecoratorRepository = contractDecoratorRepository,
            importedContractDecoratorRepository = mock(),
            blockchainService = blockchainService,
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            objectMapper = objectMapper
        )

        verify("ContractNotFoundException is thrown") {
            expectThrows<ContractNotFoundException> {
                service.importContract(PARAMS, PROJECT)
            }
        }
    }

    @Test
    fun mustThrowContractDecoratorBinaryMismatchExceptionWhenContractDecoratorBinaryMismatchesImportedContractBinary() {
        val contractDecoratorRepository = mock<ContractDecoratorRepository>()

        suppose("contract decorator will be returned") {
            call(contractDecoratorRepository.getById(CONTRACT_ID))
                .willReturn(CONTRACT_DECORATOR)
        }

        val contractMetadataRepository = mock<ContractMetadataRepository>()

        suppose("contract metadata exists") {
            call(contractMetadataRepository.exists(CONTRACT_ID, Constants.NIL_PROJECT_ID))
                .willReturn(true)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(PROJECT.chainId, PROJECT.customRpcUrl)

        suppose("contract deployment transaction will be found on blockchain") {
            call(blockchainService.findContractDeploymentTransaction(chainSpec, PARAMS.contractAddress, EVENTS))
                .willReturn(CONTRACT_DEPLOYMENT_TRANSACTION_INFO.copy(data = FunctionData("ffff")))
        }

        val service = ContractImportServiceImpl(
            abiDecoderService = EthereumAbiDecoderService(),
            contractDecompilerService = mock(),
            abiProviderService = mock(),
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = mock(),
            contractMetadataRepository = contractMetadataRepository,
            contractDecoratorRepository = contractDecoratorRepository,
            importedContractDecoratorRepository = mock(),
            blockchainService = blockchainService,
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            objectMapper = objectMapper
        )

        verify("ContractDecoratorBinaryMismatchException is thrown") {
            expectThrows<ContractDecoratorBinaryMismatchException> {
                service.importContract(PARAMS, PROJECT)
            }
        }
    }

    @Test
    fun mustCorrectlyImportContractAndCreateNewImportedContractDecorator() {
        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(PROJECT.chainId, PROJECT.customRpcUrl)

        suppose("contract deployment transaction will be found on blockchain") {
            call(blockchainService.findContractDeploymentTransaction(chainSpec, PARAMS.contractAddress, EVENTS))
                .willReturn(CONTRACT_DEPLOYMENT_TRANSACTION_INFO)
        }

        val contractDecompilerService = mock<ContractDecompilerService>()

        suppose("contract will be decompiled") {
            call(contractDecompilerService.decompile(CONTRACT_DEPLOYMENT_TRANSACTION_INFO.binary))
                .willReturn(
                    DecompiledContractJson(
                        manifest = MANIFEST_JSON,
                        artifact = ARTIFACT_JSON,
                        infoMarkdown = "infoMd"
                    )
                )
        }

        val importedContractDecoratorRepository = mock<ImportedContractDecoratorRepository>()
        val contractId = ContractId("imported-${PARAMS.contractAddress.rawValue}-${PROJECT.chainId.value}")

        suppose("imported contract decorator does not exist in the database") {
            call(importedContractDecoratorRepository.getByContractIdAndProjectId(contractId, PROJECT.id))
                .willReturn(null)
        }

        val contractDecorator = CONTRACT_DECORATOR.copy(id = contractId)
        val contractDecoratorId = ImportedContractDecoratorId(UUID.randomUUID())
        val adjustedArtifactJson = ARTIFACT_JSON.copy(
            bytecode = "$CONSTRUCTOR_BYTECODE$CONTRACT_BYTECODE",
            deployedBytecode = CONTRACT_BYTECODE
        )

        suppose("imported contract decorator will be stored into the database") {
            call(
                importedContractDecoratorRepository.store(
                    id = contractDecoratorId,
                    projectId = PROJECT.id,
                    contractId = contractId,
                    manifestJson = MANIFEST_JSON,
                    artifactJson = adjustedArtifactJson,
                    infoMarkdown = "infoMd",
                    importedAt = TestData.TIMESTAMP,
                    previewOnly = false
                )
            )
                .willReturn(contractDecorator)
        }

        val contractMetadataId = ContractMetadataId(UUID.randomUUID())
        val deployedContractId = ContractDeploymentRequestId(UUID.randomUUID())
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            call(uuidProvider.getUuid(ImportedContractDecoratorId))
                .willReturn(contractDecoratorId)
            call(uuidProvider.getUuid(ContractMetadataId))
                .willReturn(contractMetadataId)
            call(uuidProvider.getUuid(ContractDeploymentRequestId))
                .willReturn(deployedContractId)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some UTC date-time will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val contractMetadataRepository = mock<ContractMetadataRepository>()
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        val service = ContractImportServiceImpl(
            abiDecoderService = EthereumAbiDecoderService(),
            contractDecompilerService = contractDecompilerService,
            abiProviderService = mock(),
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepository,
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = importedContractDecoratorRepository,
            blockchainService = blockchainService,
            uuidProvider = uuidProvider,
            utcDateTimeProvider = utcDateTimeProvider,
            objectMapper = objectMapper
        )

        verify("contract is successfully imported") {
            expectThat(service.importContract(PARAMS.copy(contractId = null), PROJECT))
                .isEqualTo(deployedContractId)

            expectInteractions(contractMetadataRepository) {
                once.createOrUpdate(
                    ContractMetadata(
                        id = contractMetadataId,
                        name = contractDecorator.name,
                        description = contractDecorator.description,
                        contractId = contractId,
                        contractTags = contractDecorator.tags,
                        contractImplements = contractDecorator.implements,
                        projectId = PROJECT.id
                    )
                )
            }

            expectInteractions(contractDeploymentRequestRepository) {
                once.store(
                    params = StoreContractDeploymentRequestParams(
                        id = deployedContractId,
                        alias = PARAMS.alias,
                        contractId = contractId,
                        contractData = ContractBinaryData(CONTRACT_DEPLOYMENT_TRANSACTION_INFO.data.value),
                        constructorParams = CONSTRUCTOR_BYTES_32_JSON,
                        deployerAddress = CONTRACT_DEPLOYMENT_TRANSACTION_INFO.from,
                        initialEthAmount = CONTRACT_DEPLOYMENT_TRANSACTION_INFO.value,
                        chainId = PROJECT.chainId,
                        redirectUrl = "${PROJECT.baseRedirectUrl.value}/request-deploy/${deployedContractId.value}" +
                            "/action",
                        projectId = PROJECT.id,
                        createdAt = TestData.TIMESTAMP,
                        arbitraryData = PARAMS.arbitraryData,
                        screenConfig = PARAMS.screenConfig,
                        imported = true,
                        proxy = false,
                        implementationContractAddress = null
                    ),
                    metadataProjectId = PROJECT.id
                )
                once.setContractAddress(deployedContractId, PARAMS.contractAddress)
                once.setTxInfo(
                    id = deployedContractId,
                    txHash = CONTRACT_DEPLOYMENT_TRANSACTION_INFO.hash,
                    deployer = CONTRACT_DEPLOYMENT_TRANSACTION_INFO.from
                )
            }
        }
    }

    @Test
    fun mustCorrectlyImportContractAndCreateNewImportedContractDecoratorWhenOnlyContractBinaryIsFound() {
        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(PROJECT.chainId, PROJECT.customRpcUrl)

        suppose("contract deployment transaction will be found on blockchain") {
            call(blockchainService.findContractDeploymentTransaction(chainSpec, PARAMS.contractAddress, EVENTS))
                .willReturn(CONTRACT_BINARY_INFO)
        }

        val contractDecompilerService = mock<ContractDecompilerService>()

        suppose("contract will be decompiled") {
            call(contractDecompilerService.decompile(CONTRACT_BINARY_INFO.binary))
                .willReturn(
                    DecompiledContractJson(
                        manifest = MANIFEST_JSON,
                        artifact = ARTIFACT_JSON,
                        infoMarkdown = "infoMd"
                    )
                )
        }

        val importedContractDecoratorRepository = mock<ImportedContractDecoratorRepository>()
        val contractId = ContractId("imported-${PARAMS.contractAddress.rawValue}-${PROJECT.chainId.value}")

        suppose("imported contract decorator does not exist in the database") {
            call(importedContractDecoratorRepository.getByContractIdAndProjectId(contractId, PROJECT.id))
                .willReturn(null)
        }

        val contractDecorator = CONTRACT_DECORATOR.copy(id = contractId)
        val contractDecoratorId = ImportedContractDecoratorId(UUID.randomUUID())
        val adjustedArtifactJson = ARTIFACT_JSON.copy(
            bytecode = CONTRACT_BYTECODE,
            deployedBytecode = CONTRACT_BYTECODE
        )

        suppose("imported contract decorator will be stored into the database") {
            call(
                importedContractDecoratorRepository.store(
                    id = contractDecoratorId,
                    projectId = PROJECT.id,
                    contractId = contractId,
                    manifestJson = MANIFEST_JSON,
                    artifactJson = adjustedArtifactJson,
                    infoMarkdown = "infoMd",
                    importedAt = TestData.TIMESTAMP,
                    previewOnly = false
                )
            )
                .willReturn(contractDecorator)
        }

        val contractMetadataId = ContractMetadataId(UUID.randomUUID())
        val deployedContractId = ContractDeploymentRequestId(UUID.randomUUID())
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            call(uuidProvider.getUuid(ImportedContractDecoratorId))
                .willReturn(contractDecoratorId)
            call(uuidProvider.getUuid(ContractMetadataId))
                .willReturn(contractMetadataId)
            call(uuidProvider.getUuid(ContractDeploymentRequestId))
                .willReturn(deployedContractId)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some UTC date-time will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val contractMetadataRepository = mock<ContractMetadataRepository>()
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        val service = ContractImportServiceImpl(
            abiDecoderService = EthereumAbiDecoderService(),
            contractDecompilerService = contractDecompilerService,
            abiProviderService = mock(),
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepository,
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = importedContractDecoratorRepository,
            blockchainService = blockchainService,
            uuidProvider = uuidProvider,
            utcDateTimeProvider = utcDateTimeProvider,
            objectMapper = objectMapper
        )

        verify("contract is successfully imported") {
            expectThat(service.importContract(PARAMS.copy(contractId = null), PROJECT))
                .isEqualTo(deployedContractId)

            expectInteractions(contractMetadataRepository) {
                once.createOrUpdate(
                    ContractMetadata(
                        id = contractMetadataId,
                        name = contractDecorator.name,
                        description = contractDecorator.description,
                        contractId = contractId,
                        contractTags = contractDecorator.tags,
                        contractImplements = contractDecorator.implements,
                        projectId = PROJECT.id
                    )
                )
            }

            expectInteractions(contractDeploymentRequestRepository) {
                once.store(
                    params = StoreContractDeploymentRequestParams(
                        id = deployedContractId,
                        alias = PARAMS.alias,
                        contractId = contractId,
                        contractData = CONTRACT_BINARY_INFO.binary,
                        constructorParams = TestData.EMPTY_JSON_ARRAY,
                        deployerAddress = null,
                        initialEthAmount = Balance.ZERO,
                        chainId = PROJECT.chainId,
                        redirectUrl = "${PROJECT.baseRedirectUrl.value}/request-deploy/${deployedContractId.value}" +
                            "/action",
                        projectId = PROJECT.id,
                        createdAt = TestData.TIMESTAMP,
                        arbitraryData = PARAMS.arbitraryData,
                        screenConfig = PARAMS.screenConfig,
                        imported = true,
                        proxy = false,
                        implementationContractAddress = null
                    ),
                    metadataProjectId = PROJECT.id
                )
                once.setContractAddress(deployedContractId, PARAMS.contractAddress)
            }
        }
    }

    @Test
    fun mustCorrectlyImportContractAndReuseExistingImportedContractDecorator() {
        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(PROJECT.chainId, PROJECT.customRpcUrl)

        suppose("contract deployment transaction will be found on blockchain") {
            call(blockchainService.findContractDeploymentTransaction(chainSpec, PARAMS.contractAddress, EVENTS))
                .willReturn(CONTRACT_DEPLOYMENT_TRANSACTION_INFO)
        }

        val contractDecompilerService = mock<ContractDecompilerService>()

        suppose("contract will be decompiled") {
            call(contractDecompilerService.decompile(CONTRACT_DEPLOYMENT_TRANSACTION_INFO.binary))
                .willReturn(
                    DecompiledContractJson(
                        manifest = MANIFEST_JSON,
                        artifact = ARTIFACT_JSON,
                        infoMarkdown = "infoMd"
                    )
                )
        }

        val importedContractDecoratorRepository = mock<ImportedContractDecoratorRepository>()
        val contractId = ContractId("imported-${PARAMS.contractAddress.rawValue}-${PROJECT.chainId.value}")
        val contractDecorator = CONTRACT_DECORATOR.copy(id = contractId)

        suppose("imported contract decorator exists in the database") {
            call(importedContractDecoratorRepository.getByContractIdAndProjectId(contractId, PROJECT.id))
                .willReturn(contractDecorator)
        }

        val contractMetadataId = ContractMetadataId(UUID.randomUUID())
        val deployedContractId = ContractDeploymentRequestId(UUID.randomUUID())
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            call(uuidProvider.getUuid(ContractMetadataId))
                .willReturn(contractMetadataId)
            call(uuidProvider.getUuid(ContractDeploymentRequestId))
                .willReturn(deployedContractId)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some UTC date-time will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val contractMetadataRepository = mock<ContractMetadataRepository>()
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        val service = ContractImportServiceImpl(
            abiDecoderService = EthereumAbiDecoderService(),
            contractDecompilerService = contractDecompilerService,
            abiProviderService = mock(),
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepository,
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = importedContractDecoratorRepository,
            blockchainService = blockchainService,
            uuidProvider = uuidProvider,
            utcDateTimeProvider = utcDateTimeProvider,
            objectMapper = objectMapper
        )

        verify("contract is successfully imported") {
            expectThat(service.importContract(PARAMS.copy(contractId = null), PROJECT))
                .isEqualTo(deployedContractId)

            expectInteractions(contractMetadataRepository) {
                once.createOrUpdate(
                    ContractMetadata(
                        id = contractMetadataId,
                        name = contractDecorator.name,
                        description = contractDecorator.description,
                        contractId = contractId,
                        contractTags = contractDecorator.tags,
                        contractImplements = contractDecorator.implements,
                        projectId = PROJECT.id
                    )
                )
            }

            expectInteractions(contractDeploymentRequestRepository) {
                once.store(
                    params = StoreContractDeploymentRequestParams(
                        id = deployedContractId,
                        alias = PARAMS.alias,
                        contractId = contractId,
                        contractData = ContractBinaryData(CONTRACT_DEPLOYMENT_TRANSACTION_INFO.data.value),
                        constructorParams = CONSTRUCTOR_BYTES_32_JSON,
                        deployerAddress = CONTRACT_DEPLOYMENT_TRANSACTION_INFO.from,
                        initialEthAmount = CONTRACT_DEPLOYMENT_TRANSACTION_INFO.value,
                        chainId = PROJECT.chainId,
                        redirectUrl = "${PROJECT.baseRedirectUrl.value}/request-deploy/${deployedContractId.value}" +
                            "/action",
                        projectId = PROJECT.id,
                        createdAt = TestData.TIMESTAMP,
                        arbitraryData = PARAMS.arbitraryData,
                        screenConfig = PARAMS.screenConfig,
                        imported = true,
                        proxy = false,
                        implementationContractAddress = null
                    ),
                    metadataProjectId = PROJECT.id
                )
                once.setContractAddress(deployedContractId, PARAMS.contractAddress)
                once.setTxInfo(
                    id = deployedContractId,
                    txHash = CONTRACT_DEPLOYMENT_TRANSACTION_INFO.hash,
                    deployer = CONTRACT_DEPLOYMENT_TRANSACTION_INFO.from
                )
            }
        }
    }

    @Test
    fun mustCorrectlyImportProxyContractViaImplementationFunctionCall() {
        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(PROJECT.chainId, PROJECT.customRpcUrl)
        val proxyAddress = PROXY_CONTRACT_DEPLOYMENT_TRANSACTION_INFO.deployedContractAddress
        val params = PARAMS.copy(contractAddress = proxyAddress)

        suppose("proxy contract deployment transaction will be found on blockchain") {
            call(blockchainService.findContractDeploymentTransaction(chainSpec, params.contractAddress, EVENTS))
                .willReturn(PROXY_CONTRACT_DEPLOYMENT_TRANSACTION_INFO)
        }

        suppose("proxy contract slots will be empty") {
            call(blockchainService.readStorageSlot(chainSpec, params.contractAddress, PROXY_IMPLEMENTATION_SLOT))
                .willReturn(ZeroAddress.rawValue)
            call(blockchainService.readStorageSlot(chainSpec, params.contractAddress, OPENZEPPELIN_SLOT))
                .willReturn(ZeroAddress.rawValue)
            call(blockchainService.readStorageSlot(chainSpec, params.contractAddress, PROXY_BEACON_SLOT))
                .willReturn(ZeroAddress.rawValue)
        }

        val contractDecompilerService = mock<ContractDecompilerService>()

        suppose("proxy contract will be decompiled") {
            call(contractDecompilerService.decompile(PROXY_CONTRACT_DEPLOYMENT_TRANSACTION_INFO.binary))
                .willReturn(
                    DecompiledContractJson(
                        manifest = PROXY_MANIFEST_JSON,
                        artifact = PROXY_ARTIFACT_JSON,
                        infoMarkdown = "infoMd"
                    )
                )
        }

        val functionEncoderService = mock<FunctionEncoderService>()
        val encodedData = FunctionData("001122")

        suppose("proxy implementation function call will be encoded") {
            call(functionEncoderService.encode("implementation", emptyList()))
                .willReturn(encodedData)
        }

        val readOnlyParams = ExecuteReadonlyFunctionCallParams(
            contractAddress = proxyAddress,
            callerAddress = ZeroAddress.toWalletAddress(),
            functionName = "implementation",
            functionData = encodedData,
            outputParams = listOf(OutputParameter(AddressType))
        )
        val implementationAddress = ContractAddress("fde333bca111")

        suppose("implementation contract address will be returned") {
            call(blockchainService.callReadonlyFunction(chainSpec, readOnlyParams))
                .willReturn(
                    ReadonlyFunctionCallResult(
                        BlockNumber(BigInteger.ZERO),
                        TestData.TIMESTAMP,
                        implementationAddress.rawValue,
                        listOf(implementationAddress.rawValue)
                    )
                )
        }

        suppose("implementation contract deployment transaction will be found on blockchain") {
            call(blockchainService.findContractDeploymentTransaction(chainSpec, implementationAddress, EVENTS))
                .willReturn(CONTRACT_DEPLOYMENT_TRANSACTION_INFO)
        }

        suppose("implementation contract will be decompiled") {
            call(contractDecompilerService.decompile(CONTRACT_DEPLOYMENT_TRANSACTION_INFO.binary))
                .willReturn(
                    DecompiledContractJson(
                        manifest = MANIFEST_JSON,
                        artifact = ARTIFACT_JSON,
                        infoMarkdown = "infoMd"
                    )
                )
        }

        val importedContractDecoratorRepository = mock<ImportedContractDecoratorRepository>()
        val contractId = ContractId("imported-${params.contractAddress.rawValue}-${PROJECT.chainId.value}")

        suppose("imported contract decorator does not exist in the database") {
            call(importedContractDecoratorRepository.getByContractIdAndProjectId(contractId, PROJECT.id))
                .willReturn(null)
        }

        val contractDecorator = PROXY_CONTRACT_DECORATOR.copy(id = contractId)
        val contractDecoratorId = ImportedContractDecoratorId(UUID.randomUUID())
        val adjustedArtifactJson = PROXY_ARTIFACT_JSON.copy(
            bytecode = "$PROXY_CONSTRUCTOR_BYTECODE$PROXY_CONTRACT_BYTECODE",
            deployedBytecode = PROXY_CONTRACT_BYTECODE
        )

        suppose("imported contract decorator will be stored into the database") {
            call(
                importedContractDecoratorRepository.store(
                    id = contractDecoratorId,
                    projectId = PROJECT.id,
                    contractId = contractId,
                    manifestJson = PROXY_MANIFEST_JSON,
                    artifactJson = adjustedArtifactJson,
                    infoMarkdown = "infoMd",
                    importedAt = TestData.TIMESTAMP,
                    previewOnly = false
                )
            )
                .willReturn(contractDecorator)
        }

        val contractMetadataId = ContractMetadataId(UUID.randomUUID())
        val deployedContractId = ContractDeploymentRequestId(UUID.randomUUID())
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            call(uuidProvider.getUuid(ImportedContractDecoratorId))
                .willReturn(contractDecoratorId)
            call(uuidProvider.getUuid(ContractMetadataId))
                .willReturn(contractMetadataId)
            call(uuidProvider.getUuid(ContractDeploymentRequestId))
                .willReturn(deployedContractId)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some UTC date-time will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val contractMetadataRepository = mock<ContractMetadataRepository>()
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        val service = ContractImportServiceImpl(
            abiDecoderService = EthereumAbiDecoderService(),
            contractDecompilerService = contractDecompilerService,
            abiProviderService = mock(),
            functionEncoderService = functionEncoderService,
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepository,
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = importedContractDecoratorRepository,
            blockchainService = blockchainService,
            uuidProvider = uuidProvider,
            utcDateTimeProvider = utcDateTimeProvider,
            objectMapper = objectMapper
        )

        verify("contract is successfully imported") {
            expectThat(service.importContract(params.copy(contractId = null), PROJECT))
                .isEqualTo(deployedContractId)

            expectInteractions(contractMetadataRepository) {
                once.createOrUpdate(
                    ContractMetadata(
                        id = contractMetadataId,
                        name = contractDecorator.name,
                        description = contractDecorator.description,
                        contractId = contractId,
                        contractTags = contractDecorator.tags,
                        contractImplements = contractDecorator.implements,
                        projectId = PROJECT.id
                    )
                )
            }

            expectInteractions(contractDeploymentRequestRepository) {
                once.store(
                    params = StoreContractDeploymentRequestParams(
                        id = deployedContractId,
                        alias = params.alias,
                        contractId = contractId,
                        contractData = ContractBinaryData(PROXY_CONTRACT_DEPLOYMENT_TRANSACTION_INFO.data.value),
                        constructorParams = CONSTRUCTOR_BYTES_32_JSON,
                        deployerAddress = PROXY_CONTRACT_DEPLOYMENT_TRANSACTION_INFO.from,
                        initialEthAmount = PROXY_CONTRACT_DEPLOYMENT_TRANSACTION_INFO.value,
                        chainId = PROJECT.chainId,
                        redirectUrl = "${PROJECT.baseRedirectUrl.value}/request-deploy/${deployedContractId.value}" +
                            "/action",
                        projectId = PROJECT.id,
                        createdAt = TestData.TIMESTAMP,
                        arbitraryData = params.arbitraryData,
                        screenConfig = params.screenConfig,
                        imported = true,
                        proxy = true,
                        implementationContractAddress = implementationAddress
                    ),
                    metadataProjectId = PROJECT.id
                )
                once.setContractAddress(deployedContractId, params.contractAddress)
                once.setTxInfo(
                    id = deployedContractId,
                    txHash = PROXY_CONTRACT_DEPLOYMENT_TRANSACTION_INFO.hash,
                    deployer = PROXY_CONTRACT_DEPLOYMENT_TRANSACTION_INFO.from
                )
            }
        }
    }

    @Test
    fun mustCorrectlyImportProxyContractViaImplementationSlot() {
        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(PROJECT.chainId, PROJECT.customRpcUrl)
        val proxyAddress = PROXY_CONTRACT_DEPLOYMENT_TRANSACTION_INFO.deployedContractAddress
        val params = PARAMS.copy(contractAddress = proxyAddress)

        suppose("proxy contract deployment transaction will be found on blockchain") {
            call(blockchainService.findContractDeploymentTransaction(chainSpec, params.contractAddress, EVENTS))
                .willReturn(PROXY_CONTRACT_DEPLOYMENT_TRANSACTION_INFO)
        }

        val implementationAddress = ContractAddress("fde333bca111")

        suppose("proxy implementation slot will return proxy contract address") {
            call(blockchainService.readStorageSlot(chainSpec, params.contractAddress, PROXY_IMPLEMENTATION_SLOT))
                .willReturn(implementationAddress.rawValue)
        }

        suppose("proxy beacon contract slot will be empty") {
            call(blockchainService.readStorageSlot(chainSpec, params.contractAddress, PROXY_BEACON_SLOT))
                .willReturn(ZeroAddress.rawValue)
        }

        val contractDecompilerService = mock<ContractDecompilerService>()

        suppose("proxy contract will be decompiled") {
            call(contractDecompilerService.decompile(PROXY_CONTRACT_DEPLOYMENT_TRANSACTION_INFO.binary))
                .willReturn(
                    DecompiledContractJson(
                        manifest = PROXY_MANIFEST_JSON,
                        artifact = PROXY_ARTIFACT_JSON,
                        infoMarkdown = "infoMd"
                    )
                )
        }

        suppose("implementation contract deployment transaction will be found on blockchain") {
            call(blockchainService.findContractDeploymentTransaction(chainSpec, implementationAddress, EVENTS))
                .willReturn(CONTRACT_DEPLOYMENT_TRANSACTION_INFO)
        }

        suppose("implementation contract will be decompiled") {
            call(contractDecompilerService.decompile(CONTRACT_DEPLOYMENT_TRANSACTION_INFO.binary))
                .willReturn(
                    DecompiledContractJson(
                        manifest = MANIFEST_JSON,
                        artifact = ARTIFACT_JSON,
                        infoMarkdown = "infoMd"
                    )
                )
        }

        val importedContractDecoratorRepository = mock<ImportedContractDecoratorRepository>()
        val contractId = ContractId("imported-${params.contractAddress.rawValue}-${PROJECT.chainId.value}")

        suppose("imported contract decorator does not exist in the database") {
            call(importedContractDecoratorRepository.getByContractIdAndProjectId(contractId, PROJECT.id))
                .willReturn(null)
        }

        val contractDecorator = PROXY_CONTRACT_DECORATOR.copy(id = contractId)
        val contractDecoratorId = ImportedContractDecoratorId(UUID.randomUUID())
        val adjustedArtifactJson = PROXY_ARTIFACT_JSON.copy(
            bytecode = "$PROXY_CONSTRUCTOR_BYTECODE$PROXY_CONTRACT_BYTECODE",
            deployedBytecode = PROXY_CONTRACT_BYTECODE
        )

        suppose("imported contract decorator will be stored into the database") {
            call(
                importedContractDecoratorRepository.store(
                    id = contractDecoratorId,
                    projectId = PROJECT.id,
                    contractId = contractId,
                    manifestJson = PROXY_MANIFEST_JSON,
                    artifactJson = adjustedArtifactJson,
                    infoMarkdown = "infoMd",
                    importedAt = TestData.TIMESTAMP,
                    previewOnly = false
                )
            )
                .willReturn(contractDecorator)
        }

        val contractMetadataId = ContractMetadataId(UUID.randomUUID())
        val deployedContractId = ContractDeploymentRequestId(UUID.randomUUID())
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            call(uuidProvider.getUuid(ImportedContractDecoratorId))
                .willReturn(contractDecoratorId)
            call(uuidProvider.getUuid(ContractMetadataId))
                .willReturn(contractMetadataId)
            call(uuidProvider.getUuid(ContractDeploymentRequestId))
                .willReturn(deployedContractId)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some UTC date-time will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val contractMetadataRepository = mock<ContractMetadataRepository>()
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        val service = ContractImportServiceImpl(
            abiDecoderService = EthereumAbiDecoderService(),
            contractDecompilerService = contractDecompilerService,
            abiProviderService = mock(),
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepository,
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = importedContractDecoratorRepository,
            blockchainService = blockchainService,
            uuidProvider = uuidProvider,
            utcDateTimeProvider = utcDateTimeProvider,
            objectMapper = objectMapper
        )

        verify("contract is successfully imported") {
            expectThat(service.importContract(params.copy(contractId = null), PROJECT))
                .isEqualTo(deployedContractId)

            expectInteractions(contractMetadataRepository) {
                once.createOrUpdate(
                    ContractMetadata(
                        id = contractMetadataId,
                        name = contractDecorator.name,
                        description = contractDecorator.description,
                        contractId = contractId,
                        contractTags = contractDecorator.tags,
                        contractImplements = contractDecorator.implements,
                        projectId = PROJECT.id
                    )
                )
            }

            expectInteractions(contractDeploymentRequestRepository) {
                once.store(
                    params = StoreContractDeploymentRequestParams(
                        id = deployedContractId,
                        alias = params.alias,
                        contractId = contractId,
                        contractData = ContractBinaryData(PROXY_CONTRACT_DEPLOYMENT_TRANSACTION_INFO.data.value),
                        constructorParams = CONSTRUCTOR_BYTES_32_JSON,
                        deployerAddress = PROXY_CONTRACT_DEPLOYMENT_TRANSACTION_INFO.from,
                        initialEthAmount = PROXY_CONTRACT_DEPLOYMENT_TRANSACTION_INFO.value,
                        chainId = PROJECT.chainId,
                        redirectUrl = "${PROJECT.baseRedirectUrl.value}/request-deploy/${deployedContractId.value}" +
                            "/action",
                        projectId = PROJECT.id,
                        createdAt = TestData.TIMESTAMP,
                        arbitraryData = params.arbitraryData,
                        screenConfig = params.screenConfig,
                        imported = true,
                        proxy = true,
                        implementationContractAddress = implementationAddress
                    ),
                    metadataProjectId = PROJECT.id
                )
                once.setContractAddress(deployedContractId, params.contractAddress)
                once.setTxInfo(
                    id = deployedContractId,
                    txHash = PROXY_CONTRACT_DEPLOYMENT_TRANSACTION_INFO.hash,
                    deployer = PROXY_CONTRACT_DEPLOYMENT_TRANSACTION_INFO.from
                )
            }
        }
    }

    @Test
    fun mustCorrectlyImportProxyContractViaBeaconSlot() {
        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(PROJECT.chainId, PROJECT.customRpcUrl)
        val proxyAddress = PROXY_CONTRACT_DEPLOYMENT_TRANSACTION_INFO.deployedContractAddress
        val params = PARAMS.copy(contractAddress = proxyAddress)

        suppose("proxy contract deployment transaction will be found on blockchain") {
            call(blockchainService.findContractDeploymentTransaction(chainSpec, params.contractAddress, EVENTS))
                .willReturn(PROXY_CONTRACT_DEPLOYMENT_TRANSACTION_INFO)
        }

        suppose("proxy implementation slots will be empty") {
            call(blockchainService.readStorageSlot(chainSpec, params.contractAddress, PROXY_IMPLEMENTATION_SLOT))
                .willReturn(ZeroAddress.rawValue)
            call(blockchainService.readStorageSlot(chainSpec, params.contractAddress, OPENZEPPELIN_SLOT))
                .willReturn(ZeroAddress.rawValue)
        }

        val beaconAddress = ContractAddress("bbb111ccc222")

        suppose("proxy beacon contract slot will return beacon contract address") {
            call(blockchainService.readStorageSlot(chainSpec, params.contractAddress, PROXY_BEACON_SLOT))
                .willReturn(beaconAddress.rawValue)
        }

        val contractDecompilerService = mock<ContractDecompilerService>()

        suppose("proxy contract will be decompiled") {
            call(contractDecompilerService.decompile(PROXY_CONTRACT_DEPLOYMENT_TRANSACTION_INFO.binary))
                .willReturn(
                    DecompiledContractJson(
                        manifest = PROXY_MANIFEST_JSON,
                        artifact = PROXY_ARTIFACT_JSON,
                        infoMarkdown = "infoMd"
                    )
                )
        }

        val functionEncoderService = mock<FunctionEncoderService>()
        val encodedData = FunctionData("001122")

        suppose("proxy implementation function call will be encoded") {
            call(functionEncoderService.encode("implementation", emptyList()))
                .willReturn(encodedData)
        }

        val readOnlyParams = ExecuteReadonlyFunctionCallParams(
            contractAddress = beaconAddress,
            callerAddress = ZeroAddress.toWalletAddress(),
            functionName = "implementation",
            functionData = encodedData,
            outputParams = listOf(OutputParameter(AddressType))
        )
        val implementationAddress = ContractAddress("fde333bca111")

        suppose("implementation contract address will be returned") {
            call(blockchainService.callReadonlyFunction(chainSpec, readOnlyParams))
                .willReturn(
                    ReadonlyFunctionCallResult(
                        BlockNumber(BigInteger.ZERO),
                        TestData.TIMESTAMP,
                        implementationAddress.rawValue,
                        listOf(implementationAddress.rawValue)
                    )
                )
        }

        suppose("implementation contract deployment transaction will be found on blockchain") {
            call(blockchainService.findContractDeploymentTransaction(chainSpec, implementationAddress, EVENTS))
                .willReturn(CONTRACT_DEPLOYMENT_TRANSACTION_INFO)
        }

        suppose("implementation contract will be decompiled") {
            call(contractDecompilerService.decompile(CONTRACT_DEPLOYMENT_TRANSACTION_INFO.binary))
                .willReturn(
                    DecompiledContractJson(
                        manifest = MANIFEST_JSON,
                        artifact = ARTIFACT_JSON,
                        infoMarkdown = "infoMd"
                    )
                )
        }

        val importedContractDecoratorRepository = mock<ImportedContractDecoratorRepository>()
        val contractId = ContractId("imported-${params.contractAddress.rawValue}-${PROJECT.chainId.value}")

        suppose("imported contract decorator does not exist in the database") {
            call(importedContractDecoratorRepository.getByContractIdAndProjectId(contractId, PROJECT.id))
                .willReturn(null)
        }

        val contractDecorator = PROXY_CONTRACT_DECORATOR.copy(id = contractId)
        val contractDecoratorId = ImportedContractDecoratorId(UUID.randomUUID())
        val adjustedArtifactJson = PROXY_ARTIFACT_JSON.copy(
            bytecode = "$PROXY_CONSTRUCTOR_BYTECODE$PROXY_CONTRACT_BYTECODE",
            deployedBytecode = PROXY_CONTRACT_BYTECODE
        )

        suppose("imported contract decorator will be stored into the database") {
            call(
                importedContractDecoratorRepository.store(
                    id = contractDecoratorId,
                    projectId = PROJECT.id,
                    contractId = contractId,
                    manifestJson = PROXY_MANIFEST_JSON,
                    artifactJson = adjustedArtifactJson,
                    infoMarkdown = "infoMd",
                    importedAt = TestData.TIMESTAMP,
                    previewOnly = false
                )
            )
                .willReturn(contractDecorator)
        }

        val contractMetadataId = ContractMetadataId(UUID.randomUUID())
        val deployedContractId = ContractDeploymentRequestId(UUID.randomUUID())
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            call(uuidProvider.getUuid(ImportedContractDecoratorId))
                .willReturn(contractDecoratorId)
            call(uuidProvider.getUuid(ContractMetadataId))
                .willReturn(contractMetadataId)
            call(uuidProvider.getUuid(ContractDeploymentRequestId))
                .willReturn(deployedContractId)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some UTC date-time will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val contractMetadataRepository = mock<ContractMetadataRepository>()
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        val service = ContractImportServiceImpl(
            abiDecoderService = EthereumAbiDecoderService(),
            contractDecompilerService = contractDecompilerService,
            abiProviderService = mock(),
            functionEncoderService = functionEncoderService,
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = contractMetadataRepository,
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = importedContractDecoratorRepository,
            blockchainService = blockchainService,
            uuidProvider = uuidProvider,
            utcDateTimeProvider = utcDateTimeProvider,
            objectMapper = objectMapper
        )

        verify("contract is successfully imported") {
            expectThat(service.importContract(params.copy(contractId = null), PROJECT))
                .isEqualTo(deployedContractId)

            expectInteractions(contractMetadataRepository) {
                once.createOrUpdate(
                    ContractMetadata(
                        id = contractMetadataId,
                        name = contractDecorator.name,
                        description = contractDecorator.description,
                        contractId = contractId,
                        contractTags = contractDecorator.tags,
                        contractImplements = contractDecorator.implements,
                        projectId = PROJECT.id
                    )
                )
            }

            expectInteractions(contractDeploymentRequestRepository) {
                once.store(
                    params = StoreContractDeploymentRequestParams(
                        id = deployedContractId,
                        alias = params.alias,
                        contractId = contractId,
                        contractData = ContractBinaryData(PROXY_CONTRACT_DEPLOYMENT_TRANSACTION_INFO.data.value),
                        constructorParams = CONSTRUCTOR_BYTES_32_JSON,
                        deployerAddress = PROXY_CONTRACT_DEPLOYMENT_TRANSACTION_INFO.from,
                        initialEthAmount = PROXY_CONTRACT_DEPLOYMENT_TRANSACTION_INFO.value,
                        chainId = PROJECT.chainId,
                        redirectUrl = "${PROJECT.baseRedirectUrl.value}/request-deploy/${deployedContractId.value}" +
                            "/action",
                        projectId = PROJECT.id,
                        createdAt = TestData.TIMESTAMP,
                        arbitraryData = params.arbitraryData,
                        screenConfig = params.screenConfig,
                        imported = true,
                        proxy = true,
                        implementationContractAddress = implementationAddress
                    ),
                    metadataProjectId = PROJECT.id
                )
                once.setContractAddress(deployedContractId, params.contractAddress)
                once.setTxInfo(
                    id = deployedContractId,
                    txHash = PROXY_CONTRACT_DEPLOYMENT_TRANSACTION_INFO.hash,
                    deployer = PROXY_CONTRACT_DEPLOYMENT_TRANSACTION_INFO.from
                )
            }
        }
    }

    @Test
    fun mustCorrectlyCreateInputArgsForSimpleTypes() {
        val service = ContractImportServiceImpl(
            abiDecoderService = mock(),
            contractDecompilerService = mock(),
            abiProviderService = mock(),
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = mock(),
            contractMetadataRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = mock(),
            blockchainService = mock(),
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            objectMapper = mock()
        )

        verify("input args are correctly created for simple types") {
            val result = service.inputArgs(
                listOf(
                    param("uint"),
                    param("string"),
                    param("bytes"),
                    param("address")
                ),
                listOf(
                    BigInteger.ONE,
                    "test",
                    listOf("1", "2", "3"),
                    WalletAddress("123").rawValue
                )
            )

            expectThat(result)
                .isEqualTo(
                    listOf(
                        TypeAndValue("uint", BigInteger.ONE),
                        TypeAndValue("string", "test"),
                        TypeAndValue("bytes", listOf("1", "2", "3")),
                        TypeAndValue("address", WalletAddress("123").rawValue)
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyCreateInputArgsForArrayTypes() {
        val service = ContractImportServiceImpl(
            abiDecoderService = mock(),
            contractDecompilerService = mock(),
            abiProviderService = mock(),
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = mock(),
            contractMetadataRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = mock(),
            blockchainService = mock(),
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            objectMapper = mock()
        )

        verify("input args are correctly created for array types") {
            val result = service.inputArgs(
                listOf(
                    param("uint[]"),
                    param("string[][][]"),
                    param("bytes[]"),
                    param("address[]")
                ),
                listOf(
                    listOf(BigInteger.ONE, BigInteger.TWO),
                    listOf(
                        listOf(
                            listOf("test1", "test2"),
                            listOf("test3")
                        ),
                        listOf(
                            listOf("test4"),
                            listOf("test5", "test6", "test7")
                        )
                    ),
                    listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5"),
                        listOf("6", "7", "8", "9")
                    ),
                    listOf(WalletAddress("123").rawValue)
                )
            )

            expectThat(result)
                .isEqualTo(
                    listOf(
                        TypeAndValue("uint[]", listOf(BigInteger.ONE, BigInteger.TWO)),
                        TypeAndValue(
                            "string[][][]",
                            listOf(
                                listOf(
                                    listOf("test1", "test2"),
                                    listOf("test3")
                                ),
                                listOf(
                                    listOf("test4"),
                                    listOf("test5", "test6", "test7")
                                )
                            ),
                        ),
                        TypeAndValue(
                            "bytes[]",
                            listOf(
                                listOf("1", "2", "3"),
                                listOf("4", "5"),
                                listOf("6", "7", "8", "9")
                            )
                        ),
                        TypeAndValue("address[]", listOf(WalletAddress("123").rawValue))
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyCreateInputArgsForTupleType() {
        val service = ContractImportServiceImpl(
            abiDecoderService = mock(),
            contractDecompilerService = mock(),
            abiProviderService = mock(),
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = mock(),
            contractMetadataRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = mock(),
            blockchainService = mock(),
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            objectMapper = mock()
        )

        verify("input args are correctly created for tuple type") {
            val result = service.inputArgs(
                listOf(
                    param(
                        "tuple",
                        listOf(
                            param("uint"),
                            param("string"),
                            param("bytes"),
                            param("address")
                        )
                    )
                ),
                listOf(
                    tupleOf(
                        BigInteger.ONE,
                        "test",
                        listOf("1", "2", "3"),
                        WalletAddress("123").rawValue
                    )
                )
            )

            expectThat(result)
                .isEqualTo(
                    listOf(
                        TypeAndValue(
                            type = "tuple",
                            value = listOf(
                                TypeAndValue("uint", BigInteger.ONE),
                                TypeAndValue("string", "test"),
                                TypeAndValue("bytes", listOf("1", "2", "3")),
                                TypeAndValue("address", WalletAddress("123").rawValue)
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyCreateInputArgsForNestedTupleTypeArray() {
        val service = ContractImportServiceImpl(
            abiDecoderService = mock(),
            contractDecompilerService = mock(),
            abiProviderService = mock(),
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = mock(),
            contractMetadataRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = mock(),
            blockchainService = mock(),
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            objectMapper = mock()
        )

        verify("input args are correctly created for nested tuple array") {
            val result = service.inputArgs(
                listOf(
                    param(
                        "tuple[][]",
                        listOf(
                            param("uint"),
                            param("string"),
                            param("bytes"),
                            param("address")
                        )
                    )
                ),
                listOf(
                    listOf(
                        listOf(
                            tupleOf(
                                BigInteger.ZERO,
                                "test0",
                                listOf("1", "2", "3"),
                                WalletAddress("123").rawValue
                            ),
                            tupleOf(
                                BigInteger.ONE,
                                "test1",
                                listOf("4"),
                                WalletAddress("456").rawValue
                            ),
                            tupleOf(
                                BigInteger.TWO,
                                "test2",
                                listOf("5", "6"),
                                WalletAddress("789").rawValue
                            )
                        ),
                        listOf(
                            tupleOf(
                                BigInteger.TEN,
                                "test10",
                                listOf("10", "11"),
                                WalletAddress("abc").rawValue
                            ),
                            tupleOf(
                                BigInteger.ZERO,
                                "test0",
                                emptyList<String>(),
                                WalletAddress("def").rawValue
                            )
                        )
                    )
                )
            )

            expectThat(result)
                .isEqualTo(
                    listOf(
                        TypeAndValue(
                            type = "tuple[][]",
                            value = listOf(
                                listOf(
                                    listOf(
                                        TypeAndValue("uint", BigInteger.ZERO),
                                        TypeAndValue("string", "test0"),
                                        TypeAndValue("bytes", listOf("1", "2", "3")),
                                        TypeAndValue("address", WalletAddress("123").rawValue)
                                    ),
                                    listOf(
                                        TypeAndValue("uint", BigInteger.ONE),
                                        TypeAndValue("string", "test1"),
                                        TypeAndValue("bytes", listOf("4")),
                                        TypeAndValue("address", WalletAddress("456").rawValue)
                                    ),
                                    listOf(
                                        TypeAndValue("uint", BigInteger.TWO),
                                        TypeAndValue("string", "test2"),
                                        TypeAndValue("bytes", listOf("5", "6")),
                                        TypeAndValue("address", WalletAddress("789").rawValue)
                                    )
                                ),
                                listOf(
                                    listOf(
                                        TypeAndValue("uint", BigInteger.TEN),
                                        TypeAndValue("string", "test10"),
                                        TypeAndValue("bytes", listOf("10", "11")),
                                        TypeAndValue("address", WalletAddress("abc").rawValue)
                                    ),
                                    listOf(
                                        TypeAndValue("uint", BigInteger.ZERO),
                                        TypeAndValue("string", "test0"),
                                        TypeAndValue("bytes", emptyList<String>()),
                                        TypeAndValue("address", WalletAddress("def").rawValue)
                                    )
                                )
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyCreateInputArgsForNestedTupleTypeArrayWithNestedTuples() {
        val service = ContractImportServiceImpl(
            abiDecoderService = mock(),
            contractDecompilerService = mock(),
            abiProviderService = mock(),
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = mock(),
            contractMetadataRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = mock(),
            blockchainService = mock(),
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            objectMapper = mock()
        )

        verify("input args are correctly created for tuple type") {
            val result = service.inputArgs(
                listOf(
                    param(
                        "tuple[][]",
                        listOf(
                            param("uint"),
                            param("string"),
                            param("bytes"),
                            param("address"),
                            param(
                                "tuple[]",
                                listOf(
                                    param("int[]"),
                                    param(
                                        "tuple",
                                        listOf(
                                            param("bool[]"),
                                            param("string")
                                        )
                                    )
                                )
                            )
                        )
                    )
                ),
                listOf(
                    listOf(
                        listOf(
                            tupleOf(
                                BigInteger.ZERO,
                                "test0",
                                listOf("1", "2", "3"),
                                WalletAddress("123").rawValue,
                                listOf(
                                    tupleOf(
                                        listOf("100", "200"),
                                        tupleOf(
                                            listOf(true, false),
                                            "nested-string-1"
                                        )
                                    ),
                                    tupleOf(
                                        listOf("600", "700", "800"),
                                        tupleOf(
                                            listOf(false, false),
                                            "nested-string-2"
                                        )
                                    )
                                )
                            ),
                            tupleOf(
                                BigInteger.ONE,
                                "test1",
                                listOf("4"),
                                WalletAddress("456").rawValue,
                                emptyList<Tuple>()
                            ),
                            tupleOf(
                                BigInteger.TWO,
                                "test2",
                                listOf("5", "6"),
                                WalletAddress("789").rawValue,
                                listOf(
                                    tupleOf(
                                        listOf("900"),
                                        tupleOf(
                                            listOf(true, true),
                                            "nested-string-3"
                                        )
                                    )
                                )
                            )
                        ),
                        listOf(
                            tupleOf(
                                BigInteger.TEN,
                                "test10",
                                listOf("10", "11"),
                                WalletAddress("abc").rawValue,
                                listOf(
                                    tupleOf(
                                        emptyList<String>(),
                                        tupleOf(
                                            emptyList<Boolean>(),
                                            "nested-string-4"
                                        )
                                    )
                                )
                            ),
                            tupleOf(
                                BigInteger.ZERO,
                                "test0",
                                emptyList<String>(),
                                WalletAddress("def").rawValue,
                                emptyList<Tuple>()
                            )
                        )
                    )
                )
            )

            expectThat(result)
                .isEqualTo(
                    listOf(
                        TypeAndValue(
                            type = "tuple[][]",
                            value = listOf(
                                listOf(
                                    listOf(
                                        TypeAndValue("uint", BigInteger.ZERO),
                                        TypeAndValue("string", "test0"),
                                        TypeAndValue("bytes", listOf("1", "2", "3")),
                                        TypeAndValue("address", WalletAddress("123").rawValue),
                                        TypeAndValue(
                                            "tuple[]",
                                            listOf(
                                                listOf(
                                                    TypeAndValue("int[]", listOf("100", "200")),
                                                    TypeAndValue(
                                                        "tuple",
                                                        listOf(
                                                            TypeAndValue("bool[]", listOf(true, false)),
                                                            TypeAndValue("string", "nested-string-1")
                                                        )
                                                    )
                                                ),
                                                listOf(
                                                    TypeAndValue("int[]", listOf("600", "700", "800")),
                                                    TypeAndValue(
                                                        "tuple",
                                                        listOf(
                                                            TypeAndValue("bool[]", listOf(false, false)),
                                                            TypeAndValue("string", "nested-string-2")
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    ),
                                    listOf(
                                        TypeAndValue("uint", BigInteger.ONE),
                                        TypeAndValue("string", "test1"),
                                        TypeAndValue("bytes", listOf("4")),
                                        TypeAndValue("address", WalletAddress("456").rawValue),
                                        TypeAndValue("tuple[]", emptyList<Tuple>())
                                    ),
                                    listOf(
                                        TypeAndValue("uint", BigInteger.TWO),
                                        TypeAndValue("string", "test2"),
                                        TypeAndValue("bytes", listOf("5", "6")),
                                        TypeAndValue("address", WalletAddress("789").rawValue),
                                        TypeAndValue(
                                            "tuple[]",
                                            listOf(
                                                listOf(
                                                    TypeAndValue("int[]", listOf("900")),
                                                    TypeAndValue(
                                                        "tuple",
                                                        listOf(
                                                            TypeAndValue("bool[]", listOf(true, true)),
                                                            TypeAndValue("string", "nested-string-3")
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    )
                                ),
                                listOf(
                                    listOf(
                                        TypeAndValue("uint", BigInteger.TEN),
                                        TypeAndValue("string", "test10"),
                                        TypeAndValue("bytes", listOf("10", "11")),
                                        TypeAndValue("address", WalletAddress("abc").rawValue),
                                        TypeAndValue(
                                            "tuple[]",
                                            listOf(
                                                listOf(
                                                    TypeAndValue("int[]", emptyList<String>()),
                                                    TypeAndValue(
                                                        "tuple",
                                                        listOf(
                                                            TypeAndValue("bool[]", emptyList<Boolean>()),
                                                            TypeAndValue("string", "nested-string-4")
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    ),
                                    listOf(
                                        TypeAndValue("uint", BigInteger.ZERO),
                                        TypeAndValue("string", "test0"),
                                        TypeAndValue("bytes", emptyList<String>()),
                                        TypeAndValue("address", WalletAddress("def").rawValue),
                                        TypeAndValue("tuple[]", emptyList<Tuple>())
                                    )
                                )
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyPreviewAlreadyImportedContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some imported contract will be returned from contract deployment request repository") {
            call(contractDeploymentRequestRepository.getByContractAddressAndChainId(CONTRACT_ADDRESS, CHAIN_ID))
                .willReturn(IMPORTED_CONTRACT)
        }

        val contractId = IMPORTED_CONTRACT.contractId
        val decorator = ContractDecorator(
            id = contractId,
            artifact = ARTIFACT_JSON,
            manifest = MANIFEST_JSON,
            imported = true,
            interfacesProvider = null
        )

        val importedContractDecoratorRepository = mock<ImportedContractDecoratorRepository>()

        suppose("imported contract decorator is returned") {
            call(
                importedContractDecoratorRepository.getByContractIdAndProjectId(
                    contractId = contractId,
                    projectId = IMPORTED_CONTRACT.projectId
                )
            )
                .willReturn(decorator)
        }

        val service = ContractImportServiceImpl(
            abiDecoderService = EthereumAbiDecoderService(),
            contractDecompilerService = mock(),
            abiProviderService = mock(),
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = importedContractDecoratorRepository,
            blockchainService = mock(),
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            objectMapper = objectMapper
        )

        verify("contract import is correctly previewed") {
            expectThat(service.previewImport(CONTRACT_ADDRESS, ChainSpec(CHAIN_ID, null)))
                .isEqualTo(decorator)
        }
    }

    @Test
    fun mustCorrectlyPreviewDeployedContract() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("some non-imported contract will be returned from contract deployment request repository") {
            call(contractDeploymentRequestRepository.getByContractAddressAndChainId(CONTRACT_ADDRESS, CHAIN_ID))
                .willReturn(NON_IMPORTED_CONTRACT)
        }

        val contractId = NON_IMPORTED_CONTRACT.contractId
        val decorator = ContractDecorator(
            id = contractId,
            artifact = ARTIFACT_JSON,
            manifest = MANIFEST_JSON,
            imported = false,
            interfacesProvider = null
        )

        val contractDecoratorRepository = mock<ContractDecoratorRepository>()

        suppose("contract decorator is returned") {
            call(contractDecoratorRepository.getById(contractId))
                .willReturn(decorator)
        }

        val service = ContractImportServiceImpl(
            abiDecoderService = EthereumAbiDecoderService(),
            contractDecompilerService = mock(),
            abiProviderService = mock(),
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = mock(),
            contractDecoratorRepository = contractDecoratorRepository,
            importedContractDecoratorRepository = mock(),
            blockchainService = mock(),
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            objectMapper = objectMapper
        )

        verify("contract import is correctly previewed") {
            expectThat(service.previewImport(CONTRACT_ADDRESS, ChainSpec(CHAIN_ID, null)))
                .isEqualTo(decorator)
        }
    }

    @Test
    fun mustCorrectlyPreviewContractImport() {
        val contractDeploymentRequestRepository = mock<ContractDeploymentRequestRepository>()

        suppose("null will be returned from contract deployment request repository") {
            call(contractDeploymentRequestRepository.getByContractAddressAndChainId(CONTRACT_ADDRESS, CHAIN_ID))
                .willReturn(null)
        }

        val blockchainService = mock<BlockchainService>()
        val chainSpec = ChainSpec(CHAIN_ID, null)

        suppose("contract deployment transaction will be found on blockchain") {
            call(blockchainService.findContractDeploymentTransaction(chainSpec, CONTRACT_ADDRESS, EVENTS))
                .willReturn(CONTRACT_DEPLOYMENT_TRANSACTION_INFO)
        }

        val contractDecompilerService = mock<ContractDecompilerService>()

        suppose("contract will be decompiled") {
            call(contractDecompilerService.decompile(CONTRACT_DEPLOYMENT_TRANSACTION_INFO.binary))
                .willReturn(
                    DecompiledContractJson(
                        manifest = MANIFEST_JSON,
                        artifact = ARTIFACT_JSON,
                        infoMarkdown = "infoMd"
                    )
                )
        }

        val importedContractDecoratorRepository = mock<ImportedContractDecoratorRepository>()
        val contractId = ContractId("imported-${CONTRACT_ADDRESS.rawValue}-${CHAIN_ID.value}")

        suppose("imported contract decorator does not exist in the database") {
            call(importedContractDecoratorRepository.getByContractIdAndProjectId(contractId, Constants.NIL_PROJECT_ID))
                .willReturn(null)
        }

        val contractDecorator = CONTRACT_DECORATOR.copy(id = contractId)
        val contractDecoratorId = ImportedContractDecoratorId(UUID.randomUUID())
        val adjustedArtifactJson = ARTIFACT_JSON.copy(
            bytecode = "$CONSTRUCTOR_BYTECODE$CONTRACT_BYTECODE",
            deployedBytecode = CONTRACT_BYTECODE
        )

        suppose("imported contract decorator will be previewed in the database") {
            call(
                importedContractDecoratorRepository.store(
                    id = contractDecoratorId,
                    projectId = Constants.NIL_PROJECT_ID,
                    contractId = contractId,
                    manifestJson = MANIFEST_JSON,
                    artifactJson = adjustedArtifactJson,
                    infoMarkdown = "infoMd",
                    importedAt = TestData.TIMESTAMP,
                    previewOnly = true
                )
            )
                .willReturn(contractDecorator)
        }

        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            call(uuidProvider.getUuid(ImportedContractDecoratorId))
                .willReturn(contractDecoratorId)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some UTC date-time will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val service = ContractImportServiceImpl(
            abiDecoderService = EthereumAbiDecoderService(),
            contractDecompilerService = contractDecompilerService,
            abiProviderService = mock(),
            functionEncoderService = mock(),
            contractDeploymentRequestRepository = contractDeploymentRequestRepository,
            contractMetadataRepository = mock(),
            contractDecoratorRepository = mock(),
            importedContractDecoratorRepository = importedContractDecoratorRepository,
            blockchainService = blockchainService,
            uuidProvider = uuidProvider,
            utcDateTimeProvider = utcDateTimeProvider,
            objectMapper = objectMapper
        )

        verify("contract import is correctly previewed") {
            expectThat(service.previewImport(CONTRACT_ADDRESS, chainSpec))
                .isEqualTo(contractDecorator)
        }
    }

    private fun param(solidityType: String, parameters: List<ContractParameter>? = null) = ContractParameter(
        name = "",
        description = "",
        solidityName = "",
        solidityType = solidityType,
        recommendedTypes = emptyList(),
        parameters = parameters,
        hints = null
    )

    private fun tupleOf(vararg elems: Any) = Tuple(elems.toList())
}
