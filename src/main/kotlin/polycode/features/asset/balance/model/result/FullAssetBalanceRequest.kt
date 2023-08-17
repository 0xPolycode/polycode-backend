package polycode.features.asset.balance.model.result

import com.fasterxml.jackson.databind.JsonNode
import polycode.generated.jooq.id.AssetBalanceRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.model.ScreenConfig
import polycode.util.AccountBalance
import polycode.util.BlockNumber
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.SignedMessage
import polycode.util.Status
import polycode.util.UtcDateTime
import polycode.util.WalletAddress

data class FullAssetBalanceRequest(
    val id: AssetBalanceRequestId,
    val projectId: ProjectId,
    val status: Status,
    val chainId: ChainId,
    val redirectUrl: String,
    val tokenAddress: ContractAddress?,
    val blockNumber: BlockNumber?,
    val requestedWalletAddress: WalletAddress?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val balance: AccountBalance?,
    val messageToSign: String,
    val signedMessage: SignedMessage?,
    val createdAt: UtcDateTime
) {
    companion object {
        fun fromAssetBalanceRequest(
            request: AssetBalanceRequest,
            status: Status,
            balance: AccountBalance?
        ) = FullAssetBalanceRequest(
            id = request.id,
            projectId = request.projectId,
            status = status,
            chainId = request.chainId,
            redirectUrl = request.redirectUrl,
            tokenAddress = request.tokenAddress,
            blockNumber = request.blockNumber,
            requestedWalletAddress = request.requestedWalletAddress,
            arbitraryData = request.arbitraryData,
            screenConfig = request.screenConfig,
            balance = balance,
            messageToSign = request.messageToSign,
            signedMessage = request.signedMessage,
            createdAt = request.createdAt
        )
    }
}
