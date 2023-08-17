package polycode.features.contract.deployment.model.request

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import polycode.config.validation.MaxArgsSize
import polycode.config.validation.MaxJsonNodeChars
import polycode.config.validation.MaxStringSize
import polycode.config.validation.ValidAlias
import polycode.config.validation.ValidEthAddress
import polycode.config.validation.ValidUint256
import polycode.features.functions.encoding.model.FunctionArgument
import polycode.features.functions.encoding.model.FunctionArgumentSchema
import polycode.model.ScreenConfig
import polycode.util.annotation.SchemaIgnore
import polycode.util.annotation.SchemaName
import java.math.BigInteger
import javax.validation.Valid
import javax.validation.constraints.NotNull

data class CreateContractDeploymentRequest(
    @field:NotNull
    @field:ValidAlias
    val alias: String,
    @field:NotNull
    @field:MaxStringSize
    val contractId: String,
    @field:Valid
    @field:NotNull
    @field:MaxArgsSize
    @field:SchemaIgnore
    val constructorParams: List<FunctionArgument>,
    @field:ValidEthAddress
    val deployerAddress: String?,
    @field:NotNull
    @field:ValidUint256
    val initialEthAmount: BigInteger,
    @field:MaxStringSize
    val redirectUrl: String?,
    @field:MaxJsonNodeChars
    val arbitraryData: JsonNode?,
    @field:Valid
    val screenConfig: ScreenConfig?
) {
    @Suppress("unused") // used for JSON schema generation
    @JsonIgnore
    @SchemaName("constructor_params")
    private val schemaConstructorParams: List<FunctionArgumentSchema> = emptyList()
}
