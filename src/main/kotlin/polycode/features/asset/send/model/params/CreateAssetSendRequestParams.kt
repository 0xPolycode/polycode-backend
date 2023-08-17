package polycode.features.asset.send.model.params

import com.fasterxml.jackson.databind.JsonNode
import polycode.features.asset.send.model.request.CreateAssetSendRequest
import polycode.model.ScreenConfig
import polycode.util.Balance
import polycode.util.ContractAddress
import polycode.util.WalletAddress

data class CreateAssetSendRequestParams(
    val redirectUrl: String?,
    val tokenAddress: ContractAddress?,
    val assetAmount: Balance,
    val assetSenderAddress: WalletAddress?,
    val assetRecipientAddress: WalletAddress,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig
) {
    constructor(requestBody: CreateAssetSendRequest) : this(
        redirectUrl = requestBody.redirectUrl,
        tokenAddress = requestBody.tokenAddress?.let { ContractAddress(it) },
        assetAmount = Balance(requestBody.amount),
        assetSenderAddress = requestBody.senderAddress?.let { WalletAddress(it) },
        assetRecipientAddress = WalletAddress(requestBody.recipientAddress),
        arbitraryData = requestBody.arbitraryData,
        screenConfig = requestBody.screenConfig ?: ScreenConfig.EMPTY
    )
}
