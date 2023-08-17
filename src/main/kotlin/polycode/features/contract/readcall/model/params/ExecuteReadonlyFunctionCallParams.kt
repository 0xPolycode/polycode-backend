package polycode.features.contract.readcall.model.params

import com.fasterxml.jackson.databind.JsonNode
import polycode.config.validation.MaxJsonNodeChars
import polycode.config.validation.ValidationConstants
import polycode.features.contract.abi.model.AbiType
import polycode.util.ContractAddress
import polycode.util.FunctionData
import polycode.util.WalletAddress
import polycode.util.annotation.SchemaAnyOf

data class ExecuteReadonlyFunctionCallParams(
    val contractAddress: ContractAddress,
    val callerAddress: WalletAddress,
    val functionName: String,
    val functionData: FunctionData,
    val outputParams: List<OutputParameter>
)

data class OutputParameter(
    val deserializedType: AbiType,
    @field:MaxJsonNodeChars(ValidationConstants.FUNCTION_ARGUMENT_MAX_JSON_CHARS)
    val rawJson: JsonNode? = null // needed to hold pre-deserialization value
)

data class OutputParameterSchema(
    val type: String,
    val elems: List<OutputParameterTypes>
)

@SchemaAnyOf
data class OutputParameterTypes(
    val type1: OutputParameterSchema,
    val type2: String
)
