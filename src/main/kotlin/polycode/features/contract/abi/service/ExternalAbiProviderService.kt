package polycode.features.contract.abi.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import polycode.blockchain.properties.ChainSpec
import polycode.config.ApplicationProperties
import polycode.features.contract.deployment.model.json.AbiInputOutput
import polycode.features.contract.deployment.model.json.AbiObject
import polycode.features.contract.deployment.model.json.ArtifactJson
import polycode.features.contract.deployment.model.json.ConstructorDecorator
import polycode.features.contract.deployment.model.json.EventDecorator
import polycode.features.contract.deployment.model.json.EventTypeDecorator
import polycode.features.contract.deployment.model.json.FunctionDecorator
import polycode.features.contract.deployment.model.json.ManifestJson
import polycode.features.contract.deployment.model.json.ReturnTypeDecorator
import polycode.features.contract.deployment.model.json.TypeDecorator
import polycode.features.contract.importing.model.json.DecompiledContractJson
import polycode.util.ContractAddress

@Service
@Suppress("TooManyFunctions")
class ExternalAbiProviderService(
    private val basicJsonRestTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    private val applicationProperties: ApplicationProperties
) : AbiProviderService {

    companion object : KLogging() {
        private const val QUERY_PARAMS =
            "?module=contract&action=getsourcecode&address={contractAddress}&apikey={apiKey}"

        private data class Response(
            val status: String?,
            val message: String?,
            val result: List<ContractSourceResponse>?
        )

        private data class ContractSourceResponse(
            @JsonProperty("ABI")
            val abi: String?,
            @JsonProperty("ContractName")
            val contractName: String?
        )

        @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
        private data class ExternalAbiObject(
            val anonymous: Boolean?,
            val inputs: List<ExternalAbiInputOutput>?,
            val outputs: List<ExternalAbiInputOutput>?,
            val stateMutability: String?,
            val name: String?,
            val type: String
        ) {
            fun toAbiObject(): AbiObject = AbiObject(
                anonymous = anonymous,
                inputs = inputs?.map { it.toAbiInputOutput() },
                outputs = outputs?.map { it.toAbiInputOutput() },
                stateMutability = stateMutability,
                name = name,
                type = type
            )
        }

        @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
        private data class ExternalAbiInputOutput(
            val components: List<ExternalAbiInputOutput>?,
            val name: String,
            val type: String,
            val indexed: Boolean?
        ) {
            fun toAbiInputOutput(): AbiInputOutput = AbiInputOutput(
                components = components?.map { it.toAbiInputOutput() },
                internalType = type,
                name = name,
                type = type,
                indexed = indexed
            )
        }
    }

    override fun getContractAbi(
        bytecode: String,
        deployedBytecode: String,
        contractAddress: ContractAddress,
        chainSpec: ChainSpec
    ): DecompiledContractJson? {
        val chainProperties = applicationProperties.chain[chainSpec.chainId]
        val chainExplorerApiUrl = chainProperties?.chainExplorerApiUrl
        val chainExplorerApiKey = chainProperties?.chainExplorerApiKey

        return if (chainExplorerApiUrl != null && chainExplorerApiKey != null) {
            getCode(
                contractAddress = contractAddress.rawValue,
                apiUrl = chainExplorerApiUrl,
                apiKey = chainExplorerApiKey
            )?.let { contractSource ->
                contractSource.abi
                    ?.let { objectMapper.tryReadAbi(it) }
                    ?.let { abiJson ->
                        ArtifactJson(
                            contractName = contractSource.contractName ?: "ImportedContract",
                            sourceName = "ImportedContract.sol",
                            abi = abiJson,
                            bytecode = bytecode,
                            deployedBytecode = deployedBytecode,
                            linkReferences = null,
                            deployedLinkReferences = null
                        )
                    }
                    ?.withGeneratedManifest()
            }?.cleanupManifestSignatures()
        } else {
            logger.debug { "Chain explorer not set for chainSpec: $chainSpec" }
            null
        }
    }

    private fun getCode(contractAddress: String, apiUrl: String, apiKey: String): ContractSourceResponse? =
        try {
            basicJsonRestTemplate.getForEntity(
                apiUrl + QUERY_PARAMS.replace("{contractAddress}", contractAddress).replace("{apiKey}", apiKey),
                Response::class.java
            ).body?.result?.firstOrNull()
        } catch (e: RestClientException) {
            logger.warn(e) { "Fetching contract code failed, contractAddress: $contractAddress, apiUrl: $apiUrl" }
            null
        }

    private fun ObjectMapper.tryReadAbi(abi: String): List<AbiObject>? =
        try {
            readValue(abi, Array<ExternalAbiObject>::class.java)?.toList()?.map { it.toAbiObject() }
        } catch (e: JsonProcessingException) {
            logger.warn(e) { "Unable to decode ABI string: $abi" }
            null
        }

    private fun ArtifactJson.withGeneratedManifest() = DecompiledContractJson(
        manifest = ManifestJson(
            name = contractName,
            description = "",
            tags = emptySet(),
            implements = emptySet(),
            eventDecorators = decorateEvents(),
            constructorDecorators = decorateConstructors(),
            functionDecorators = decorateFunctions()
        ),
        artifact = this,
        infoMarkdown = null
    )

    private fun ArtifactJson.decorateEvents(): List<EventDecorator> = abi.filter { it.type == "event" }
        .map { abiObject ->
            EventDecorator(
                signature = abiObject.toSignature(""),
                name = abiObject.name ?: "",
                description = "",
                parameterDecorators = abiObject.inputs.orEmpty().map { it.toEventTypeDecorator() }
            )
        }

    private fun ArtifactJson.decorateConstructors(): List<ConstructorDecorator> =
        abi.filter { it.type == "constructor" }
            .map { abiObject ->
                ConstructorDecorator(
                    signature = abiObject.toSignature("constructor"),
                    description = "",
                    parameterDecorators = abiObject.inputs.orEmpty().map { it.toTypeDecorator() }
                )
            }

    private fun ArtifactJson.decorateFunctions(): List<FunctionDecorator> = abi.filter { it.type == "function" }
        .map { abiObject ->
            FunctionDecorator(
                signature = abiObject.toSignature(""),
                name = abiObject.name ?: "",
                description = "",
                parameterDecorators = abiObject.inputs.orEmpty().map { it.toTypeDecorator() },
                returnDecorators = abiObject.outputs.orEmpty().map { it.toReturnTypeDecorator() },
                emittableEvents = emptyList(),
                readOnly = abiObject.stateMutability == "view" || abiObject.stateMutability == "pure"
            )
        }

    private fun AbiObject.toSignature(defaultName: String) =
        (name ?: defaultName) + inputs.orEmpty().toParenString { it.toSignature() }

    private fun <T> List<T>.toParenString(transform: ((T) -> CharSequence)?) =
        joinToString(separator = ",", prefix = "(", postfix = ")", transform = transform)

    private fun AbiInputOutput.toSignature(): String =
        if (type.startsWith("tuple") && components != null) {
            components.toParenString { it.toSignature() } +
                type.removePrefix("tuple")
        } else type

    private fun AbiInputOutput.toEventTypeDecorator(): EventTypeDecorator =
        EventTypeDecorator(
            name = name,
            description = "",
            indexed = indexed ?: false,
            recommendedTypes = emptyList(),
            parameters = components?.map { it.toTypeDecorator() },
            hints = emptyList()
        )

    private fun AbiInputOutput.toTypeDecorator(): TypeDecorator =
        TypeDecorator(
            name = name,
            description = "",
            recommendedTypes = emptyList(),
            parameters = components?.map { it.toTypeDecorator() },
            hints = emptyList()
        )

    private fun AbiInputOutput.toReturnTypeDecorator(): ReturnTypeDecorator =
        ReturnTypeDecorator(
            name = name,
            description = "",
            solidityType = type,
            recommendedTypes = emptyList(),
            parameters = components?.map { it.toReturnTypeDecorator() },
            hints = emptyList()
        )

    private fun DecompiledContractJson.cleanupManifestSignatures(): DecompiledContractJson {
        val constructors = manifest.constructorDecorators.map { it.copy(signature = it.signature.cleanupSignature()) }
        val functions = manifest.functionDecorators.map { it.copy(signature = it.signature.cleanupSignature()) }
        val events = manifest.eventDecorators.map { it.copy(signature = it.signature.cleanupSignature()) }

        return copy(
            manifest = manifest.copy(
                constructorDecorators = constructors,
                functionDecorators = functions,
                eventDecorators = events
            )
        )
    }

    private fun String.cleanupSignature(): String {
        val (name, rest) = split('(', limit = 2)
        val params = rest.removeSuffix(")")
        return "$name(${params.replace("(", "tuple(")})"
    }
}
