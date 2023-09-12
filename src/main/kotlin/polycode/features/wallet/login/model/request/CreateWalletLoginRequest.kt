package polycode.features.wallet.login.model.request

import polycode.config.validation.ValidEthAddress
import javax.validation.constraints.NotNull

data class CreateWalletLoginRequest(
    @field:NotNull
    @field:ValidEthAddress
    val walletAddress: String
)
