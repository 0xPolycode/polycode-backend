package polycode.features.contract.importing.service

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import org.springframework.stereotype.Service
import polycode.blockchain.BlockchainService
import polycode.blockchain.properties.ChainSpec
import polycode.exception.ContractDecoratorBinaryMismatchException
import polycode.exception.ContractNotFoundException
import polycode.exception.ResourceNotFoundException
import polycode.features.api.access.model.result.Project
import polycode.features.contract.abi.model.AddressType
import polycode.features.contract.abi.model.Tuple
import polycode.features.contract.abi.service.AbiDecoderService
import polycode.features.contract.abi.service.AbiProviderService
import polycode.features.contract.deployment.model.json.ArtifactJson
import polycode.features.contract.deployment.model.json.ManifestJson
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
import polycode.features.contract.readcall.model.params.ExecuteReadonlyFunctionCallParams
import polycode.features.contract.readcall.model.params.OutputParameter
import polycode.features.functions.encoding.service.FunctionEncoderService
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.generated.jooq.id.ContractMetadataId
import polycode.generated.jooq.id.ImportedContractDecoratorId
import polycode.generated.jooq.id.ProjectId
import polycode.model.DeserializableEvent
import polycode.model.result.ContractBinaryInfo
import polycode.model.result.ContractDeploymentTransactionInfo
import polycode.model.result.ContractMetadata
import polycode.model.result.FullContractDeploymentTransactionInfo
import polycode.service.UtcDateTimeProvider
import polycode.service.UuidProvider
import polycode.util.Constants
import polycode.util.ContractAddress
import polycode.util.ContractBinaryData
import polycode.util.ContractId
import polycode.util.EthStorageSlot
import polycode.util.FunctionData
import polycode.util.ZeroAddress
import kotlin.math.min

@Service
@Suppress("LongParameterList", "TooManyFunctions")
class ContractImportServiceImpl(
    private val abiDecoderService: AbiDecoderService,
    private val contractDecompilerService: ContractDecompilerService,
    private val abiProviderService: AbiProviderService,
    private val functionEncoderService: FunctionEncoderService,
    private val contractDeploymentRequestRepository: ContractDeploymentRequestRepository,
    private val contractMetadataRepository: ContractMetadataRepository,
    private val contractDecoratorRepository: ContractDecoratorRepository,
    private val importedContractDecoratorRepository: ImportedContractDecoratorRepository,
    private val blockchainService: BlockchainService,
    private val uuidProvider: UuidProvider,
    private val utcDateTimeProvider: UtcDateTimeProvider,
    private val objectMapper: ObjectMapper
) : ContractImportService {

    companion object : KLogging() {
        private const val ETH_VALUE_LENGTH = 64
        private const val PROXY_FUNCTION_NAME = "implementation"

        private val PROXY_IMPLEMENTATION_SLOTS =
            listOf(
                // slot according to standard: https://eips.ethereum.org/EIPS/eip-1967
                EthStorageSlot("0x360894a13ba1a3210667c828492db98dca3e2076cc3735a920a3ca505d382bbc"),
                // slot used by older OpenZeppelin proxies (keccak256("org.zeppelinos.proxy.implementation"))
                EthStorageSlot("0x7050c9e0f4ca769c69bd3a8ef740bc37934f8e2c036e5a723fd8ee048ed3f8c3")
            )
        private val PROXY_BEACON_SLOTS = listOf(
            // slot according to standard: https://eips.ethereum.org/EIPS/eip-1967
            EthStorageSlot("0xa3f0ad74e5423aebfd80d3ef4346578335a9a72aeaee59ff6cb3582b35133d50")
        )

        private data class DecompiledContract(
            val contractId: ContractId,
            val decorator: ContractDecorator,
            val constructorParams: String,
            val contractDeploymentTransactionInfo: ContractDeploymentTransactionInfo,
            val implementationAddress: ContractAddress?
        )

        private data class OutputParams(val params: List<OutputParameter>)
        data class TypeAndValue(val type: String, val value: Any)
    }

    override fun importExistingContract(params: ImportContractParams, project: Project): ContractDeploymentRequestId? {
        logger.info { "Attempting to import existing smart contract, params: $params, project: $project" }

        return contractDeploymentRequestRepository.getByContractAddressAndChainId(
            contractAddress = params.contractAddress,
            chainId = project.chainId
        )?.let {
            logger.info { "Already existing contract found, params: $params, project: $project" }

            if (it.imported) {
                copyImportedContract(params, project, it)
            } else {
                copyDeployedContract(params, project, it)
            }
        }
    }

    override fun importContract(
        params: ImportContractParams,
        project: Project
    ): ContractDeploymentRequestId {
        logger.info { "Importing smart contract, params: $params, project: $project" }

        return if (params.contractId != null) {
            importContractWithExistingDecorator(params, project, params.contractId)
        } else {
            val chainSpec = ChainSpec(
                chainId = project.chainId,
                customRpcUrl = project.customRpcUrl
            )
            val decompiledContract = decompileContract(
                importContractAddress = params.contractAddress,
                chainSpec = chainSpec,
                projectId = project.id,
                previewDecorator = false
            )

            storeImportedContract(decompiledContract, params, project)
        }
    }

    override fun previewImport(contractAddress: ContractAddress, chainSpec: ChainSpec): ContractDecorator {
        logger.info { "Preview for contract import, contractAddress: $contractAddress, chainSpec: $chainSpec" }

        val decorator = contractDeploymentRequestRepository.getByContractAddressAndChainId(
            contractAddress = contractAddress,
            chainId = chainSpec.chainId
        )?.let {
            logger.info { "Already existing contract found, contractAddress: $contractAddress, chainSpec: $chainSpec" }

            if (it.imported) {
                importedContractDecoratorRepository.getByContractIdAndProjectId(it.contractId, it.projectId)
            } else {
                contractDecoratorRepository.getById(it.contractId)
            }
        }

        return decorator ?: decompileContract(
            importContractAddress = contractAddress,
            chainSpec = chainSpec,
            projectId = Constants.NIL_PROJECT_ID,
            previewDecorator = true
        ).decorator
    }

    private fun copyDeployedContract(
        params: ImportContractParams,
        project: Project,
        request: ContractDeploymentRequest
    ): ContractDeploymentRequestId {
        val id = uuidProvider.getUuid(ContractDeploymentRequestId)
        val storedRequest = contractDeploymentRequestRepository.store(
            params = StoreContractDeploymentRequestParams.fromContractDeploymentRequest(
                id = id,
                importContractParams = params,
                contractDeploymentRequest = request,
                project = project,
                createdAt = utcDateTimeProvider.getUtcDateTime(),
                imported = false
            ),
            metadataProjectId = Constants.NIL_PROJECT_ID
        )

        if (request.txHash != null && request.deployerAddress != null) {
            contractDeploymentRequestRepository.setTxInfo(storedRequest.id, request.txHash, request.deployerAddress)
        }

        contractDeploymentRequestRepository.setContractAddress(storedRequest.id, params.contractAddress)

        return storedRequest.id
    }

    private fun copyImportedContract(
        params: ImportContractParams,
        project: Project,
        request: ContractDeploymentRequest
    ): ContractDeploymentRequestId {
        val existingManifestJson = importedContractDecoratorRepository.getManifestJsonByContractIdAndProjectId(
            contractId = request.contractId,
            projectId = request.projectId
        )!!
        val existingArtifactJson = importedContractDecoratorRepository.getArtifactJsonByContractIdAndProjectId(
            contractId = request.contractId,
            projectId = request.projectId
        )!!
        val existingInfoMarkdown = importedContractDecoratorRepository.getInfoMarkdownByContractIdAndProjectId(
            contractId = request.contractId,
            projectId = request.projectId
        )!!

        val newContractId = ContractId("imported-${params.contractAddress.rawValue}-${project.chainId.value}")
        val newDecorator = importedContractDecoratorRepository.getByContractIdAndProjectId(
            contractId = newContractId,
            projectId = project.id
        ) ?: importedContractDecoratorRepository.store(
            id = uuidProvider.getUuid(ImportedContractDecoratorId),
            projectId = project.id,
            contractId = newContractId,
            manifestJson = existingManifestJson,
            artifactJson = existingArtifactJson,
            infoMarkdown = existingInfoMarkdown,
            importedAt = utcDateTimeProvider.getUtcDateTime(),
            previewOnly = false
        )

        contractMetadataRepository.createOrUpdate(
            ContractMetadata(
                id = uuidProvider.getUuid(ContractMetadataId),
                name = newDecorator.name,
                description = newDecorator.description,
                contractId = newContractId,
                contractTags = newDecorator.tags,
                contractImplements = newDecorator.implements,
                projectId = project.id
            )
        )

        val id = uuidProvider.getUuid(ContractDeploymentRequestId)
        val storedRequest = contractDeploymentRequestRepository.store(
            params = StoreContractDeploymentRequestParams.fromContractDeploymentRequest(
                id = id,
                importContractParams = params,
                contractDeploymentRequest = request.copy(contractId = newContractId),
                project = project,
                createdAt = utcDateTimeProvider.getUtcDateTime(),
                imported = true
            ),
            metadataProjectId = project.id
        )

        if (request.txHash != null && request.deployerAddress != null) {
            contractDeploymentRequestRepository.setTxInfo(id, request.txHash, request.deployerAddress)
        }

        contractDeploymentRequestRepository.setContractAddress(id, params.contractAddress)

        return storedRequest.id
    }

    @Suppress("ThrowsCount")
    private fun importContractWithExistingDecorator(
        params: ImportContractParams,
        project: Project,
        contractId: ContractId
    ): ContractDeploymentRequestId {
        val decoratorNotFoundMessage = "Contract decorator not found for contract ID: ${contractId.value}"

        val contractDecorator = contractDecoratorRepository.getById(contractId)
            ?: throw ResourceNotFoundException(decoratorNotFoundMessage)

        if (!contractMetadataRepository.exists(contractId, Constants.NIL_PROJECT_ID)) {
            throw ResourceNotFoundException(decoratorNotFoundMessage)
        }

        val chainSpec = ChainSpec(
            chainId = project.chainId,
            customRpcUrl = project.customRpcUrl
        )
        val contractDeploymentTransactionInfo = findContractDeploymentTransaction(
            contractAddress = params.contractAddress,
            chainSpec = chainSpec,
            events = contractDecorator.getDeserializableEvents(objectMapper)
        )

        val decoratorBinary = contractDecorator.binary.withPrefix
        val deployedBinary = when (contractDeploymentTransactionInfo) {
            is FullContractDeploymentTransactionInfo -> contractDeploymentTransactionInfo.data.value
            is ContractBinaryInfo -> contractDeploymentTransactionInfo.binary.withPrefix
        }

        if (deployedBinary.startsWith(decoratorBinary).not()) {
            throw ContractDecoratorBinaryMismatchException(params.contractAddress, contractId)
        }

        val constructorParams = deployedBinary.removePrefix(decoratorBinary)
        val constructorInputs = contractDecorator.constructors.firstOrNull()?.inputs.orEmpty()

        return storeContractDeploymentRequest(
            constructorParams = constructorParams,
            constructorInputs = constructorInputs,
            params = params,
            contractId = contractId,
            contractDeploymentTransactionInfo = contractDeploymentTransactionInfo,
            project = project,
            metadataProjectId = Constants.NIL_PROJECT_ID,
            proxy = false,
            implementationContractAddress = null
        )
    }

    private fun decompileContract(
        importContractAddress: ContractAddress,
        chainSpec: ChainSpec,
        projectId: ProjectId,
        previewDecorator: Boolean
    ): DecompiledContract {
        val contractDeploymentTransactionInfo = findContractDeploymentTransaction(
            contractAddress = importContractAddress,
            chainSpec = chainSpec,
            events = emptyList()
        )

        val (fullBinary, shortBinary) = when (contractDeploymentTransactionInfo) {
            is FullContractDeploymentTransactionInfo ->
                Pair(contractDeploymentTransactionInfo.data.value, contractDeploymentTransactionInfo.binary.value)

            is ContractBinaryInfo ->
                Pair(contractDeploymentTransactionInfo.binary.value, contractDeploymentTransactionInfo.binary.value)
        }
        val constructorParamsStart = min(fullBinary.indexOf(shortBinary) + shortBinary.length, fullBinary.length)

        val constructorParams = fullBinary.substring(constructorParamsStart)

        val (decompiledContract, implementationAddress) = getOrDecompileAbi(
            bytecode = fullBinary,
            deployedBytecode = ContractBinaryData(shortBinary),
            contractAddress = importContractAddress,
            chainSpec = chainSpec
        ).let {
            it.copy(
                artifact = it.artifact.copy(
                    bytecode = FunctionData(fullBinary).withoutPrefix.removeSuffix(constructorParams),
                    deployedBytecode = shortBinary
                )
            ).resolveProxyContract(importContractAddress, chainSpec)
        }
        val contractAddress = contractDeploymentTransactionInfo.deployedContractAddress
        val contractId = ContractId("imported-${contractAddress.rawValue}-${chainSpec.chainId.value}")

        val contractDecorator =
            importedContractDecoratorRepository.getByContractIdAndProjectId(contractId, projectId)
                ?: importedContractDecoratorRepository.store(
                    id = uuidProvider.getUuid(ImportedContractDecoratorId),
                    projectId = projectId,
                    contractId = contractId,
                    manifestJson = decompiledContract.manifest,
                    artifactJson = decompiledContract.artifact,
                    infoMarkdown = decompiledContract.infoMarkdown ?: "",
                    importedAt = utcDateTimeProvider.getUtcDateTime(),
                    previewOnly = previewDecorator
                )

        return DecompiledContract(
            contractId = contractId,
            decorator = contractDecorator,
            constructorParams = constructorParams,
            contractDeploymentTransactionInfo = contractDeploymentTransactionInfo,
            implementationAddress = implementationAddress
        )
    }

    private fun storeImportedContract(
        decompiledContract: DecompiledContract,
        params: ImportContractParams,
        project: Project
    ): ContractDeploymentRequestId {
        contractMetadataRepository.createOrUpdate(
            ContractMetadata(
                id = uuidProvider.getUuid(ContractMetadataId),
                name = decompiledContract.decorator.name,
                description = decompiledContract.decorator.description,
                contractId = decompiledContract.contractId,
                contractTags = decompiledContract.decorator.tags,
                contractImplements = decompiledContract.decorator.implements,
                projectId = project.id
            )
        )

        val constructorInputs = List(decompiledContract.constructorParams.length / ETH_VALUE_LENGTH) {
            ContractParameter(
                name = "",
                description = "",
                solidityName = "",
                solidityType = "bytes32",
                recommendedTypes = emptyList(),
                parameters = null,
                hints = null
            )
        }

        return storeContractDeploymentRequest(
            constructorParams = decompiledContract.constructorParams,
            constructorInputs = constructorInputs,
            params = params,
            contractId = decompiledContract.contractId,
            contractDeploymentTransactionInfo = decompiledContract.contractDeploymentTransactionInfo,
            project = project,
            metadataProjectId = project.id,
            proxy = decompiledContract.implementationAddress != null,
            implementationContractAddress = decompiledContract.implementationAddress
        )
    }

    private fun findContractDeploymentTransaction(
        contractAddress: ContractAddress,
        chainSpec: ChainSpec,
        events: List<DeserializableEvent>
    ) = blockchainService.findContractDeploymentTransaction(
        chainSpec = chainSpec,
        contractAddress = contractAddress,
        events = events
    ) ?: throw ContractNotFoundException(contractAddress)

    private fun DecompiledContractJson.resolveProxyContract(
        importContractAddress: ContractAddress,
        chainSpec: ChainSpec
    ): Pair<DecompiledContractJson, ContractAddress?> =
        if (this.manifest.functionDecorators.any { it.signature == "$PROXY_FUNCTION_NAME()" }) {
            val implementationAddress = findContractProxyImplementation(importContractAddress, chainSpec)
            val implementationTransactionInfo = findContractDeploymentTransaction(
                contractAddress = implementationAddress,
                chainSpec = chainSpec,
                events = emptyList()
            )
            val decompiledImplementation = getOrDecompileAbi(
                bytecode = implementationTransactionInfo.binary.value,
                deployedBytecode = implementationTransactionInfo.binary,
                contractAddress = implementationAddress,
                chainSpec = chainSpec
            )

            val implManifest = decompiledImplementation.manifest
            val eventDecorators = this.manifest.eventDecorators + implManifest.eventDecorators
            val constructorDecorators = this.manifest.constructorDecorators + implManifest.constructorDecorators
            val functionDecorators = this.manifest.functionDecorators + implManifest.functionDecorators

            val mergedJson = DecompiledContractJson(
                manifest = ManifestJson(
                    name = this.manifest.name,
                    description = this.manifest.description,
                    tags = this.manifest.tags,
                    implements = this.manifest.implements,
                    eventDecorators = eventDecorators.distinct(),
                    constructorDecorators = constructorDecorators.distinct(),
                    functionDecorators = functionDecorators.distinct()
                ),
                artifact = ArtifactJson(
                    contractName = this.artifact.contractName,
                    sourceName = this.artifact.sourceName,
                    abi = (this.artifact.abi + decompiledImplementation.artifact.abi).distinct(),
                    bytecode = this.artifact.bytecode,
                    deployedBytecode = this.artifact.deployedBytecode,
                    linkReferences = this.artifact.linkReferences,
                    deployedLinkReferences = this.artifact.deployedLinkReferences
                ),
                infoMarkdown = this.infoMarkdown
            )

            Pair(mergedJson, implementationAddress)
        } else Pair(this, null)

    private fun findContractProxyImplementation(
        contractAddress: ContractAddress,
        chainSpec: ChainSpec
    ): ContractAddress {
        val implementationValue = PROXY_IMPLEMENTATION_SLOTS.readSlots(chainSpec, contractAddress)
        val beaconValue = implementationValue ?: PROXY_BEACON_SLOTS.readSlots(chainSpec, contractAddress)

        return implementationValue
            ?: beaconValue?.readProxyImplementationFunction(chainSpec)
            ?: contractAddress.readProxyImplementationFunction(chainSpec)
    }

    private fun List<EthStorageSlot>.readSlots(
        chainSpec: ChainSpec,
        contractAddress: ContractAddress
    ): ContractAddress? = asSequence()
        .map {
            blockchainService.readStorageSlot(
                chainSpec = chainSpec,
                contractAddress = contractAddress,
                slot = it
            ).let(::ContractAddress)
        }
        .filter { it != ZeroAddress.toContractAddress() }
        .firstOrNull()

    private fun ContractAddress.readProxyImplementationFunction(chainSpec: ChainSpec): ContractAddress {
        val data = functionEncoderService.encode(
            functionName = PROXY_FUNCTION_NAME,
            arguments = emptyList()
        )

        val implementation = blockchainService.callReadonlyFunction(
            chainSpec = chainSpec,
            params = ExecuteReadonlyFunctionCallParams(
                contractAddress = this,
                callerAddress = ZeroAddress.toWalletAddress(),
                functionName = PROXY_FUNCTION_NAME,
                functionData = data,
                outputParams = listOf(OutputParameter(AddressType))
            )
        )

        return ContractAddress(implementation.returnValues[0].toString())
    }

    @Suppress("LongParameterList")
    private fun storeContractDeploymentRequest(
        constructorParams: String,
        constructorInputs: List<ContractParameter>,
        params: ImportContractParams,
        contractId: ContractId,
        contractDeploymentTransactionInfo: ContractDeploymentTransactionInfo,
        project: Project,
        metadataProjectId: ProjectId,
        proxy: Boolean,
        implementationContractAddress: ContractAddress?
    ): ContractDeploymentRequestId {
        val constructorInputTypes = constructorInputs
            .joinToString(separator = ",") { it.toSolidityTypeJson() }
            .let { objectMapper.readValue("{\"params\":[$it]}", OutputParams::class.java) }
            .params

        val decodedConstructorParams = abiDecoderService.decode(
            types = constructorInputTypes.map { it.deserializedType },
            encodedInput = constructorParams
        )

        val id = uuidProvider.getUuid(ContractDeploymentRequestId)
        val storeParams = StoreContractDeploymentRequestParams.fromImportedContract(
            id = id,
            params = params,
            contractId = contractId,
            contractDeploymentTransactionInfo = contractDeploymentTransactionInfo,
            constructorParams = objectMapper.valueToTree(inputArgs(constructorInputs, decodedConstructorParams)),
            project = project,
            createdAt = utcDateTimeProvider.getUtcDateTime(),
            proxy = proxy,
            implementationContractAddress = implementationContractAddress
        )

        contractDeploymentRequestRepository.store(storeParams, metadataProjectId)
        contractDeploymentRequestRepository.setContractAddress(
            id = id,
            contractAddress = contractDeploymentTransactionInfo.deployedContractAddress
        )

        if (contractDeploymentTransactionInfo is FullContractDeploymentTransactionInfo) {
            contractDeploymentRequestRepository.setTxInfo(
                id = id,
                txHash = contractDeploymentTransactionInfo.hash,
                deployer = contractDeploymentTransactionInfo.from
            )
        }
        return id
    }

    private fun ContractParameter.toSolidityTypeJson(): String =
        if (solidityType.startsWith("tuple")) {
            val elems = parameters.orEmpty().joinToString(separator = ",") { it.toSolidityTypeJson() }
            "{\"type\":\"$solidityType\",\"elems\":[$elems]}"
        } else {
            "\"$solidityType\""
        }

    internal fun inputArgs(inputTypes: List<ContractParameter>, decodedValues: List<Any>): List<TypeAndValue> =
        inputTypes.zip(decodedValues).map {
            TypeAndValue(
                type = it.first.solidityType,
                value = it.second.deepMap { tuple ->
                    inputArgs(it.first.parameters.orEmpty(), tuple.elems)
                }
            )
        }

    private fun Any.deepMap(mapFn: (Tuple) -> List<Any>): Any =
        when (this) {
            is List<*> -> this.map { it!!.deepMap(mapFn) }
            is Tuple -> mapFn(this)
            else -> this
        }

    private fun getOrDecompileAbi(
        bytecode: String,
        deployedBytecode: ContractBinaryData,
        contractAddress: ContractAddress,
        chainSpec: ChainSpec
    ): DecompiledContractJson =
        abiProviderService.getContractAbi(
            bytecode = bytecode,
            deployedBytecode = deployedBytecode.value,
            contractAddress = contractAddress,
            chainSpec = chainSpec
        ) ?: contractDecompilerService.decompile(deployedBytecode)
}
