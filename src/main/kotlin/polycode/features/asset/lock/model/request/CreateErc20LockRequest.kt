package polycode.features.asset.lock.model.request

import com.fasterxml.jackson.databind.JsonNode
import polycode.config.validation.MaxJsonNodeChars
import polycode.config.validation.MaxStringSize
import polycode.config.validation.ValidEthAddress
import polycode.config.validation.ValidUint256
import polycode.model.ScreenConfig
import java.math.BigInteger
import javax.validation.Valid
import javax.validation.constraints.NotNull

data class CreateErc20LockRequest(
    @field:MaxStringSize
    val redirectUrl: String?,
    @field:NotNull
    @field:ValidEthAddress
    val tokenAddress: String,
    @field:NotNull
    @field:ValidUint256
    val amount: BigInteger,
    @field:NotNull
    @field:ValidUint256
    val lockDurationInSeconds: BigInteger,
    @field:NotNull
    @field:ValidEthAddress
    val lockContractAddress: String,
    @field:ValidEthAddress
    val senderAddress: String?,
    @field:MaxJsonNodeChars
    val arbitraryData: JsonNode?,
    @field:Valid
    val screenConfig: ScreenConfig?
)
