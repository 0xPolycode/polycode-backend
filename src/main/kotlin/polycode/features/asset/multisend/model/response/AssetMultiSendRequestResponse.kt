package polycode.features.asset.multisend.model.response

import com.fasterxml.jackson.databind.JsonNode
import polycode.features.asset.multisend.model.result.AssetMultiSendRequest
import polycode.generated.jooq.id.AssetMultiSendRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.model.ScreenConfig
import polycode.model.response.EventInfoResponse
import polycode.model.response.TransactionResponse
import polycode.util.AssetType
import polycode.util.Balance
import polycode.util.Status
import polycode.util.WithFunctionDataOrEthValue
import polycode.util.WithMultiTransactionData
import java.math.BigInteger
import java.time.OffsetDateTime

data class AssetMultiSendRequestResponse(
    val id: AssetMultiSendRequestId,
    val projectId: ProjectId,
    val approveStatus: Status?,
    val disperseStatus: Status?,
    val chainId: Long,
    val tokenAddress: String?,
    val disperseContractAddress: String,
    val assetType: AssetType,
    val items: List<MultiSendItemResponse>,
    val senderAddress: String?,
    val arbitraryData: JsonNode?,
    val approveScreenConfig: ScreenConfig?,
    val disperseScreenConfig: ScreenConfig?,
    val redirectUrl: String,
    val approveTx: TransactionResponse?,
    val disperseTx: TransactionResponse?,
    val createdAt: OffsetDateTime,
    val approveEvents: List<EventInfoResponse>?,
    val disperseEvents: List<EventInfoResponse>?
) {
    constructor(request: WithFunctionDataOrEthValue<AssetMultiSendRequest>) : this(
        id = request.value.id,
        projectId = request.value.projectId,
        approveStatus = if (request.value.tokenAddress == null) null else Status.PENDING,
        disperseStatus = if (request.value.tokenAddress != null) null else Status.PENDING,
        chainId = request.value.chainId.value,
        tokenAddress = request.value.tokenAddress?.rawValue,
        disperseContractAddress = request.value.disperseContractAddress.rawValue,
        assetType = if (request.value.tokenAddress != null) AssetType.TOKEN else AssetType.NATIVE,
        items = request.value.assetAmounts
            .zip(request.value.assetRecipientAddresses)
            .zip(request.value.itemNames)
            .map {
                MultiSendItemResponse(
                    walletAddress = it.first.second.rawValue,
                    amount = it.first.first.rawValue,
                    itemName = it.second
                )
            },
        senderAddress = request.value.assetSenderAddress?.rawValue,
        arbitraryData = request.value.arbitraryData,
        approveScreenConfig = request.value.approveScreenConfig.orEmpty(),
        disperseScreenConfig = request.value.disperseScreenConfig.orEmpty(),
        redirectUrl = request.value.redirectUrl,
        approveTx = request.value.tokenAddress?.let {
            TransactionResponse.unmined(
                from = request.value.assetSenderAddress,
                to = it,
                data = request.data,
                value = Balance.ZERO
            )
        },
        disperseTx = if (request.value.tokenAddress == null) {
            TransactionResponse.unmined(
                from = request.value.assetSenderAddress,
                to = request.value.disperseContractAddress,
                data = request.data,
                value = request.ethValue ?: Balance.ZERO
            )
        } else null,
        createdAt = request.value.createdAt.value,
        approveEvents = null,
        disperseEvents = null
    )

    constructor(request: WithMultiTransactionData<AssetMultiSendRequest>) : this(
        id = request.value.id,
        projectId = request.value.projectId,
        approveStatus = request.approveStatus,
        disperseStatus = request.disperseStatus,
        chainId = request.value.chainId.value,
        tokenAddress = request.value.tokenAddress?.rawValue,
        disperseContractAddress = request.value.disperseContractAddress.rawValue,
        assetType = if (request.value.tokenAddress != null) AssetType.TOKEN else AssetType.NATIVE,
        items = request.value.assetAmounts
            .zip(request.value.assetRecipientAddresses)
            .zip(request.value.itemNames)
            .map {
                MultiSendItemResponse(
                    walletAddress = it.first.second.rawValue,
                    amount = it.first.first.rawValue,
                    itemName = it.second
                )
            },
        senderAddress = request.value.assetSenderAddress?.rawValue,
        arbitraryData = request.value.arbitraryData,
        approveScreenConfig = request.value.approveScreenConfig.orEmpty(),
        disperseScreenConfig = request.value.disperseScreenConfig.orEmpty(),
        redirectUrl = request.value.redirectUrl,
        approveTx = request.approveTransactionData?.let { TransactionResponse(it) },
        disperseTx = request.disperseTransactionData?.let { TransactionResponse(it) },
        createdAt = request.value.createdAt.value,
        approveEvents = request.approveTransactionData?.events?.map { EventInfoResponse(it) },
        disperseEvents = request.disperseTransactionData?.events?.map { EventInfoResponse(it) }
    )
}

data class MultiSendItemResponse(
    val walletAddress: String,
    val amount: BigInteger,
    val itemName: String?
)
