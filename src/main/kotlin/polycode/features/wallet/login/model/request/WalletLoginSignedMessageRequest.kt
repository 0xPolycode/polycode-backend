package polycode.features.wallet.login.model.request

import polycode.config.validation.MaxStringSize
import javax.validation.constraints.NotNull

data class WalletLoginSignedMessageRequest(
    @field:NotNull
    @field:MaxStringSize
    val signedMessage: String
)
