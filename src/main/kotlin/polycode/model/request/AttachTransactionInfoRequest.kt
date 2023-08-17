package polycode.model.request

import polycode.config.validation.ValidEthAddress
import polycode.config.validation.ValidEthTxHash
import javax.validation.constraints.NotNull

data class AttachTransactionInfoRequest(
    @field:NotNull
    @field:ValidEthTxHash
    val txHash: String,
    @field:NotNull
    @field:ValidEthAddress
    val callerAddress: String
)
