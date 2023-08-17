package polycode.features.asset.balance.model.params

import com.fasterxml.jackson.databind.JsonNode
import polycode.features.asset.balance.model.request.CreateAssetBalanceRequest
import polycode.model.ScreenConfig
import polycode.util.BlockNumber
import polycode.util.ContractAddress
import polycode.util.WalletAddress

data class CreateAssetBalanceRequestParams(
    val redirectUrl: String?,
    val tokenAddress: ContractAddress?,
    val blockNumber: BlockNumber?,
    val requestedWalletAddress: WalletAddress?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig
) {
    constructor(requestBody: CreateAssetBalanceRequest) : this(
        redirectUrl = requestBody.redirectUrl,
        tokenAddress = requestBody.tokenAddress?.let { ContractAddress(it) },
        blockNumber = requestBody.blockNumber?.let { BlockNumber(it) },
        requestedWalletAddress = requestBody.walletAddress?.let { WalletAddress(it) },
        arbitraryData = requestBody.arbitraryData,
        screenConfig = requestBody.screenConfig ?: ScreenConfig.EMPTY
    )
}
