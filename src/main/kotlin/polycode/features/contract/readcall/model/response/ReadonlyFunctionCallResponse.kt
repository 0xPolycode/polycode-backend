package polycode.features.contract.readcall.model.response

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import polycode.features.contract.readcall.model.params.OutputParameterSchema
import polycode.features.contract.readcall.model.result.ReadonlyFunctionCallResult
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.util.WithDeployedContractIdAndAddress
import polycode.util.annotation.SchemaAnyOf
import polycode.util.annotation.SchemaIgnore
import polycode.util.annotation.SchemaName
import java.math.BigInteger
import java.time.OffsetDateTime

data class ReturnValueTypes(
    val types: RecursiveReturnValueTypes
)

@SchemaAnyOf
data class RecursiveReturnValueTypes(
    val type1: String,
    val type2: Boolean,
    val type3: List<ReturnValueTypes>
)

data class ReadonlyFunctionCallResponse(
    val deployedContractId: ContractDeploymentRequestId?,
    val contractAddress: String,
    val blockNumber: BigInteger,
    val timestamp: OffsetDateTime,
    @SchemaIgnore
    val outputParams: JsonNode,
    @SchemaIgnore
    val returnValues: List<Any>,
    val rawReturnValue: String
) {
    companion object {
        operator fun invoke(
            result: WithDeployedContractIdAndAddress<ReadonlyFunctionCallResult>,
            outputParams: JsonNode
        ) = ReadonlyFunctionCallResponse(
            deployedContractId = result.deployedContractId,
            contractAddress = result.contractAddress.rawValue,
            blockNumber = result.value.blockNumber.value,
            timestamp = result.value.timestamp.value,
            outputParams = outputParams,
            returnValues = result.value.returnValues,
            rawReturnValue = result.value.rawReturnValue
        )
    }

    @Suppress("unused") // used for JSON schema generation
    @JsonIgnore
    @SchemaName("output_params")
    private val schemaOutputStructParams: List<OutputParameterSchema> = emptyList()

    @Suppress("unused") // used for JSON schema generation
    @JsonIgnore
    @SchemaName("return_values")
    private val schemaReturnValues: List<ReturnValueTypes> = emptyList()
}
