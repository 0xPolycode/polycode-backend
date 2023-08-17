package polycode.features.wallet.authorization.model.params

import com.fasterxml.jackson.databind.JsonNode
import polycode.features.wallet.authorization.model.request.CreateAuthorizationRequest
import polycode.model.ScreenConfig
import polycode.util.WalletAddress

data class CreateAuthorizationRequestParams(
    val requestedWalletAddress: WalletAddress?,
    val redirectUrl: String?,
    val messageToSign: String?,
    val storeIndefinitely: Boolean,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig
) {
    constructor(requestBody: CreateAuthorizationRequest) : this(
        requestedWalletAddress = requestBody.walletAddress?.let { WalletAddress(it) },
        redirectUrl = requestBody.redirectUrl,
        messageToSign = requestBody.messageToSign,
        storeIndefinitely = requestBody.storeIndefinitely ?: true,
        arbitraryData = requestBody.arbitraryData,
        screenConfig = requestBody.screenConfig ?: ScreenConfig.EMPTY
    )
}
