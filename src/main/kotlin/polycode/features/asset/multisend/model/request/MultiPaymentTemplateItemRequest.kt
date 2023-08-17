package polycode.features.asset.multisend.model.request

import polycode.config.validation.MaxStringSize
import polycode.config.validation.ValidEthAddress
import polycode.config.validation.ValidUint256
import java.math.BigInteger
import javax.validation.constraints.NotNull

data class MultiPaymentTemplateItemRequest(
    @field:NotNull
    @field:ValidEthAddress
    val walletAddress: String,
    @field:MaxStringSize
    val itemName: String?,
    @field:NotNull
    @field:ValidUint256
    val amount: BigInteger
)
