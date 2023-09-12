package polycode.features.wallet.login.model.params

import polycode.features.wallet.login.model.request.CreateWalletLoginRequest
import polycode.util.WalletAddress

data class CreateWalletLoginRequestParams(
    val walletAddress: WalletAddress,
) {
    constructor(requestBody: CreateWalletLoginRequest) : this(
        walletAddress = WalletAddress(requestBody.walletAddress)
    )
}
