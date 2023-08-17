package polycode.features.contract.arbitrarycall.model.request

import com.fasterxml.jackson.databind.JsonNode
import polycode.config.validation.MaxFunctionDataSize
import polycode.config.validation.MaxJsonNodeChars
import polycode.config.validation.MaxStringSize
import polycode.config.validation.ValidEthAddress
import polycode.config.validation.ValidUint256
import polycode.features.contract.deployment.model.params.DeployedContractIdentifierRequestBody
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.model.ScreenConfig
import java.math.BigInteger
import javax.validation.Valid
import javax.validation.constraints.NotNull

data class CreateContractArbitraryCallRequest(
    override val deployedContractId: ContractDeploymentRequestId?,
    @field:MaxStringSize
    override val deployedContractAlias: String?,
    @field:ValidEthAddress
    override val contractAddress: String?,
    @field:NotNull
    @field:MaxFunctionDataSize
    val functionData: String,
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
) : DeployedContractIdentifierRequestBody
