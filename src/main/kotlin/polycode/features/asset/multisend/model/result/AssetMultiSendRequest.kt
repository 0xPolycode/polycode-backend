package polycode.features.asset.multisend.model.result

import com.fasterxml.jackson.databind.JsonNode
import polycode.generated.jooq.id.AssetMultiSendRequestId
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
import polycode.util.WithMultiTransactionData

data class AssetMultiSendRequest(
    val id: AssetMultiSendRequestId,
    val projectId: ProjectId,
    val chainId: ChainId,
    val redirectUrl: String,
    val tokenAddress: ContractAddress?,
    val disperseContractAddress: ContractAddress,
    val assetAmounts: List<Balance>,
    val assetRecipientAddresses: List<WalletAddress>,
    val itemNames: List<String?>,
    val assetSenderAddress: WalletAddress?,
    val approveTxHash: TransactionHash?,
    val disperseTxHash: TransactionHash?,
    val arbitraryData: JsonNode?,
    val approveScreenConfig: ScreenConfig,
    val disperseScreenConfig: ScreenConfig,
    val createdAt: UtcDateTime
) {
    @Suppress("LongParameterList")
    fun withMultiTransactionData(
        approveStatus: Status?,
        approveData: FunctionData?,
        approveTransactionInfo: BlockchainTransactionInfo?,
        disperseStatus: Status?,
        disperseData: FunctionData?,
        disperseValue: Balance?,
        disperseTransactionInfo: BlockchainTransactionInfo?
    ): WithMultiTransactionData<AssetMultiSendRequest> =
        WithMultiTransactionData(
            value = this,
            approveStatus = approveStatus,
            approveTransactionData = tokenAddress?.let {
                TransactionData(
                    txHash = this.approveTxHash,
                    transactionInfo = approveTransactionInfo,
                    fromAddress = this.assetSenderAddress,
                    toAddress = it,
                    data = approveData,
                    value = Balance.ZERO
                )
            },
            disperseStatus = disperseStatus,
            disperseTransactionData = if (disperseStatus != null) {
                TransactionData(
                    txHash = this.disperseTxHash,
                    transactionInfo = disperseTransactionInfo,
                    fromAddress = this.assetSenderAddress,
                    toAddress = this.disperseContractAddress,
                    data = disperseData,
                    value = disperseValue
                )
            } else null
        )
}
