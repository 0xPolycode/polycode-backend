package polycode.model.request

import polycode.config.validation.MaxStringSize
import polycode.config.validation.ValidEthAddress
import javax.validation.constraints.NotNull

data class AttachSignedMessageRequest(
    @field:NotNull
    @field:ValidEthAddress
    val walletAddress: String,
    @field:NotNull
    @field:MaxStringSize
    val signedMessage: String
)
