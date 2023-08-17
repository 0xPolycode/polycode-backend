package polycode.features.asset.send.model.result

import com.fasterxml.jackson.databind.JsonNode
import polycode.generated.jooq.id.AssetSendRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.model.ScreenConfig
import polycode.model.result.BlockchainTransactionInfo
import polycode.util.Balance
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.FunctionData
import polycode.util.Status
import polycode.util.TransactionData
import polycode.util.TransactionHash
import polycode.util.UtcDateTime
import polycode.util.WalletAddress
import polycode.util.WithTransactionData

data class AssetSendRequest(
    val id: AssetSendRequestId,
    val projectId: ProjectId,
    val chainId: ChainId,
    val redirectUrl: String,
    val tokenAddress: ContractAddress?,
    val assetAmount: Balance,
    val assetSenderAddress: WalletAddress?,
    val assetRecipientAddress: WalletAddress,
    val txHash: TransactionHash?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val createdAt: UtcDateTime
) {
    fun withTransactionData(
        status: Status,
        data: FunctionData?,
        value: Balance?,
        transactionInfo: BlockchainTransactionInfo?
    ): WithTransactionData<AssetSendRequest> =
        WithTransactionData(
            value = this,
            status = status,
            transactionData = TransactionData(
                txHash = this.txHash,
                transactionInfo = transactionInfo,
                fromAddress = this.assetSenderAddress,
                toAddress = this.tokenAddress ?: this.assetRecipientAddress,
                data = data,
                value = value
            )
        )
}
