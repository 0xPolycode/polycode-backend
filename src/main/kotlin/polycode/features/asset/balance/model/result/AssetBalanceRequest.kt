package polycode.features.asset.balance.model.result

import com.fasterxml.jackson.databind.JsonNode
import polycode.generated.jooq.id.AssetBalanceRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.model.ScreenConfig
import polycode.util.BlockNumber
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.SignedMessage
import polycode.util.UtcDateTime
import polycode.util.WalletAddress

data class AssetBalanceRequest(
    val id: AssetBalanceRequestId,
    val projectId: ProjectId,
    val chainId: ChainId,
    val redirectUrl: String,
    val tokenAddress: ContractAddress?,
    val blockNumber: BlockNumber?,
    val requestedWalletAddress: WalletAddress?,
    val actualWalletAddress: WalletAddress?,
    val signedMessage: SignedMessage?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val createdAt: UtcDateTime
) {
    val messageToSign: String
        get() = "Verification message ID to sign: ${id.value}"
}
