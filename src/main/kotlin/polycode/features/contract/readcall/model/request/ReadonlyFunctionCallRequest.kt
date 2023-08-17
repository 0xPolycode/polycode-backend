package polycode.features.contract.readcall.model.request

import com.fasterxml.jackson.annotation.JsonIgnore
import polycode.config.validation.MaxArgsSize
import polycode.config.validation.MaxStringSize
import polycode.config.validation.ValidEthAddress
import polycode.config.validation.ValidUint256
import polycode.features.contract.deployment.model.params.DeployedContractIdentifierRequestBody
import polycode.features.contract.readcall.model.params.OutputParameter
import polycode.features.contract.readcall.model.params.OutputParameterSchema
import polycode.features.functions.encoding.model.FunctionArgument
import polycode.features.functions.encoding.model.FunctionArgumentSchema
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.util.annotation.SchemaIgnore
import polycode.util.annotation.SchemaName
import java.math.BigInteger
import javax.validation.Valid
import javax.validation.constraints.NotNull

data class ReadonlyFunctionCallRequest(
    override val deployedContractId: ContractDeploymentRequestId?,
    @field:MaxStringSize
    override val deployedContractAlias: String?,
    @field:ValidEthAddress
    override val contractAddress: String?,
    @field:ValidUint256
    val blockNumber: BigInteger?,
    @field:NotNull
    @field:MaxStringSize
    val functionName: String,
    @field:Valid
    @field:NotNull
    @field:MaxArgsSize
    @field:SchemaIgnore
    val functionParams: List<FunctionArgument>,
    @field:Valid
    @field:NotNull
    @field:MaxArgsSize
    @field:SchemaIgnore
    val outputParams: List<OutputParameter>,
    @field:NotNull
    @field:ValidEthAddress
    val callerAddress: String
) : DeployedContractIdentifierRequestBody {
    @Suppress("unused") // used for JSON schema generation
    @JsonIgnore
    @SchemaName("function_params")
    private val schemaFunctionParams: List<FunctionArgumentSchema> = emptyList()

    @Suppress("unused") // used for JSON schema generation
    @JsonIgnore
    @SchemaName("output_params")
    private val schemaOutputStructParams: List<OutputParameterSchema> = emptyList()
}
