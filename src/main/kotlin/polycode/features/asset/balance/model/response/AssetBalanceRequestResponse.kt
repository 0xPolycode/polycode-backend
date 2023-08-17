package polycode.features.asset.balance.model.response

import com.fasterxml.jackson.databind.JsonNode
import polycode.features.asset.balance.model.result.AssetBalanceRequest
import polycode.features.asset.balance.model.result.FullAssetBalanceRequest
import polycode.generated.jooq.id.AssetBalanceRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.model.ScreenConfig
import polycode.util.AssetType
import polycode.util.Status
import java.math.BigInteger
import java.time.OffsetDateTime

data class AssetBalanceRequestResponse(
    val id: AssetBalanceRequestId,
    val projectId: ProjectId,
    val status: Status,
    val chainId: Long,
    val redirectUrl: String,
    val tokenAddress: String?,
    val assetType: AssetType,
    val blockNumber: BigInteger?,
    val walletAddress: String?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig?,
    val balance: BalanceResponse?,
    val messageToSign: String,
    val signedMessage: String?,
    val createdAt: OffsetDateTime
) {
    constructor(balanceRequest: AssetBalanceRequest) : this(
        id = balanceRequest.id,
        projectId = balanceRequest.projectId,
        status = Status.PENDING,
        chainId = balanceRequest.chainId.value,
        redirectUrl = balanceRequest.redirectUrl,
        tokenAddress = balanceRequest.tokenAddress?.rawValue,
        assetType = if (balanceRequest.tokenAddress != null) AssetType.TOKEN else AssetType.NATIVE,
        blockNumber = balanceRequest.blockNumber?.value,
        walletAddress = balanceRequest.requestedWalletAddress?.rawValue,
        arbitraryData = balanceRequest.arbitraryData,
        screenConfig = balanceRequest.screenConfig.orEmpty(),
        balance = null,
        messageToSign = balanceRequest.messageToSign,
        signedMessage = balanceRequest.signedMessage?.value,
        createdAt = balanceRequest.createdAt.value
    )

    constructor(balanceRequest: FullAssetBalanceRequest) : this(
        id = balanceRequest.id,
        projectId = balanceRequest.projectId,
        status = balanceRequest.status,
        chainId = balanceRequest.chainId.value,
        redirectUrl = balanceRequest.redirectUrl,
        tokenAddress = balanceRequest.tokenAddress?.rawValue,
        assetType = if (balanceRequest.tokenAddress != null) AssetType.TOKEN else AssetType.NATIVE,
        blockNumber = balanceRequest.blockNumber?.value,
        walletAddress = balanceRequest.requestedWalletAddress?.rawValue,
        arbitraryData = balanceRequest.arbitraryData,
        screenConfig = balanceRequest.screenConfig.orEmpty(),
        balance = balanceRequest.balance?.let {
            BalanceResponse(
                wallet = it.wallet.rawValue,
                blockNumber = it.blockNumber.value,
                timestamp = it.timestamp.value,
                amount = it.amount.rawValue
            )
        },
        messageToSign = balanceRequest.messageToSign,
        signedMessage = balanceRequest.signedMessage?.value,
        createdAt = balanceRequest.createdAt.value
    )
}

data class BalanceResponse(
    val wallet: String,
    val blockNumber: BigInteger,
    val timestamp: OffsetDateTime,
    val amount: BigInteger
)
