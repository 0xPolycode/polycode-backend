package polycode.features.asset.send.model.response

import com.fasterxml.jackson.databind.JsonNode
import polycode.features.asset.send.model.result.AssetSendRequest
import polycode.generated.jooq.id.AssetSendRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.model.ScreenConfig
import polycode.model.response.EventInfoResponse
import polycode.model.response.TransactionResponse
import polycode.util.AssetType
import polycode.util.Balance
import polycode.util.Status
import polycode.util.WithFunctionDataOrEthValue
import polycode.util.WithTransactionData
import java.math.BigInteger
import java.time.OffsetDateTime

data class AssetSendRequestResponse(
    val id: AssetSendRequestId,
    val projectId: ProjectId,
    val status: Status,
    val chainId: Long,
    val tokenAddress: String?,
    val assetType: AssetType,
    val amount: BigInteger,
    val senderAddress: String?,
    val recipientAddress: String,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig?,
    val redirectUrl: String,
    val sendTx: TransactionResponse,
    val createdAt: OffsetDateTime,
    val events: List<EventInfoResponse>?
) {
    constructor(sendRequest: WithFunctionDataOrEthValue<AssetSendRequest>) : this(
        id = sendRequest.value.id,
        projectId = sendRequest.value.projectId,
        status = Status.PENDING,
        chainId = sendRequest.value.chainId.value,
        tokenAddress = sendRequest.value.tokenAddress?.rawValue,
        assetType = if (sendRequest.value.tokenAddress != null) AssetType.TOKEN else AssetType.NATIVE,
        amount = sendRequest.value.assetAmount.rawValue,
        senderAddress = sendRequest.value.assetSenderAddress?.rawValue,
        recipientAddress = sendRequest.value.assetRecipientAddress.rawValue,
        arbitraryData = sendRequest.value.arbitraryData,
        screenConfig = sendRequest.value.screenConfig.orEmpty(),
        redirectUrl = sendRequest.value.redirectUrl,
        sendTx = TransactionResponse.unmined(
            from = sendRequest.value.assetSenderAddress,
            to = sendRequest.value.tokenAddress ?: sendRequest.value.assetRecipientAddress,
            data = sendRequest.data,
            value = sendRequest.ethValue ?: Balance.ZERO
        ),
        createdAt = sendRequest.value.createdAt.value,
        events = null
    )

    constructor(sendRequest: WithTransactionData<AssetSendRequest>) : this(
        id = sendRequest.value.id,
        projectId = sendRequest.value.projectId,
        status = sendRequest.status,
        chainId = sendRequest.value.chainId.value,
        tokenAddress = sendRequest.value.tokenAddress?.rawValue,
        assetType = if (sendRequest.value.tokenAddress != null) AssetType.TOKEN else AssetType.NATIVE,
        amount = sendRequest.value.assetAmount.rawValue,
        senderAddress = sendRequest.value.assetSenderAddress?.rawValue,
        recipientAddress = sendRequest.value.assetRecipientAddress.rawValue,
        arbitraryData = sendRequest.value.arbitraryData,
        screenConfig = sendRequest.value.screenConfig.orEmpty(),
        redirectUrl = sendRequest.value.redirectUrl,
        sendTx = TransactionResponse(sendRequest.transactionData),
        createdAt = sendRequest.value.createdAt.value,
        events = sendRequest.transactionData.events?.map { EventInfoResponse(it) }
    )
}
