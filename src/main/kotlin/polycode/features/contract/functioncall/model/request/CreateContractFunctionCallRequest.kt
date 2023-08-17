package polycode.features.contract.functioncall.model.request

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import polycode.config.validation.MaxArgsSize
import polycode.config.validation.MaxJsonNodeChars
import polycode.config.validation.MaxStringSize
import polycode.config.validation.ValidEthAddress
import polycode.config.validation.ValidUint256
import polycode.features.contract.deployment.model.params.DeployedContractIdentifierRequestBody
import polycode.features.functions.encoding.model.FunctionArgument
import polycode.features.functions.encoding.model.FunctionArgumentSchema
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.model.ScreenConfig
import polycode.util.annotation.SchemaIgnore
import polycode.util.annotation.SchemaName
import java.math.BigInteger
import javax.validation.Valid
import javax.validation.constraints.NotNull

data class CreateContractFunctionCallRequest(
    override val deployedContractId: ContractDeploymentRequestId?,
    @field:MaxStringSize
    override val deployedContractAlias: String?,
    @field:ValidEthAddress
    override val contractAddress: String?,
    @field:NotNull
    @field:MaxStringSize
    val functionName: String,
    @field:Valid
    @field:NotNull
    @field:MaxArgsSize
    @field:SchemaIgnore
    val functionParams: List<FunctionArgument>,
    @field:NotNull
    @field:ValidUint256
    val ethAmount: BigInteger,
    @field:MaxStringSize
    val redirectUrl: String?,
    @field:MaxJsonNodeChars
    val arbitraryData: JsonNode?,
    @field:Valid
    val screenConfig: ScreenConfig?,
    @field:ValidEthAddress
    val callerAddress: String?
) : DeployedContractIdentifierRequestBody {
    @Suppress("unused") // used for JSON schema generation
    @JsonIgnore
    @SchemaName("function_params")
    private val schemaFunctionParams: List<FunctionArgumentSchema> = emptyList()
}
