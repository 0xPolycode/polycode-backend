package polycode.features.functions.decoding.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import polycode.config.ContractManifestServiceProperties
import polycode.exception.AbiDecodingException
import polycode.features.contract.abi.model.Tuple
import polycode.features.contract.abi.service.AbiDecoderService
import polycode.features.contract.deployment.model.json.AbiInputOutput
import polycode.features.contract.readcall.model.params.OutputParameter
import polycode.features.functions.decoding.model.EthFunction
import polycode.features.functions.encoding.model.FunctionArgument
import polycode.util.FunctionData

@Service
class ExternalFunctionDecoderService(
    private val externalContractDecompilerServiceRestTemplate: RestTemplate,
    private val contractManifestServiceProperties: ContractManifestServiceProperties,
    private val abiDecoderService: AbiDecoderService,
    private val objectMapper: ObjectMapper
) : FunctionDecoderService {

    companion object : KLogging() {
        private const val SIGNATURE_PLACEHOLDER = "{signature}"
        private const val SIGNATURE_LENGTH = 10

        private data class Response(
            val name: String,
            val inputs: List<AbiInputOutput>
        )

        // TODO duplicate in ContractImportServiceImpl
        private data class OutputParams(val params: List<OutputParameter>)
        private data class TypeAndValue(val type: String, val value: Any)
    }

    override fun decode(data: FunctionData): EthFunction? {
        val signature = data.value.take(SIGNATURE_LENGTH)
        val callData = data.value.drop(SIGNATURE_LENGTH)

        val responseBody: Response? = try {
            externalContractDecompilerServiceRestTemplate.getForEntity(
                contractManifestServiceProperties.functionSignaturePath.replace(SIGNATURE_PLACEHOLDER, signature),
                Response::class.java
            ).body
        } catch (e: RestClientException) {
            null
        }

        return responseBody?.let {
            val functionInputTypes = responseBody.inputs
                .joinToString(separator = ",") { it.toSolidityTypeJson() }
                .let { objectMapper.readValue("{\"params\":[$it]}", OutputParams::class.java) }
                .params

            val decodedFunctionParams = try {
                abiDecoderService.decode(
                    types = functionInputTypes.map { it.deserializedType },
                    encodedInput = callData
                )
            } catch (e: AbiDecodingException) {
                logger.warn(e) {
                    "Cannot decode contract function params, callData: $callData," +
                        " functionInputTypes: $functionInputTypes"
                }
                null
            }

            EthFunction(
                name = responseBody.name,
                arguments = decodedFunctionParams?.let {
                    objectMapper.valueToTree<ArrayNode>(inputArgs(responseBody.inputs, decodedFunctionParams)).map {
                        objectMapper.treeToValue(it, FunctionArgument::class.java)
                    }
                }
            )
        }
    }

    private fun AbiInputOutput.toSolidityTypeJson(): String =
        if (type.startsWith("tuple")) {
            val elems = components.orEmpty().joinToString(separator = ",") { it.toSolidityTypeJson() }
            "{\"type\":\"$type\",\"elems\":[$elems]}"
        } else {
            "\"$type\""
        }

    private fun inputArgs(inputTypes: List<AbiInputOutput>, decodedValues: List<Any>): List<TypeAndValue> =
        inputTypes.zip(decodedValues).map {
            TypeAndValue(
                type = it.first.type,
                value = it.second.deepMap { tuple ->
                    inputArgs(it.first.components.orEmpty(), tuple.elems)
                }
            )
        }

    private fun Any.deepMap(mapFn: (Tuple) -> List<Any>): Any =
        when (this) {
            is List<*> -> this.map { it!!.deepMap(mapFn) }
            is Tuple -> mapFn(this)
            else -> this
        }
}
