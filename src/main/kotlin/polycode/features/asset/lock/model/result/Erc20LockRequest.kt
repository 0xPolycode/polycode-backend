package polycode.features.asset.lock.model.result

import com.fasterxml.jackson.databind.JsonNode
import polycode.generated.jooq.id.Erc20LockRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.model.ScreenConfig
import polycode.model.result.BlockchainTransactionInfo
import polycode.util.Balance
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.DurationSeconds
import polycode.util.FunctionData
import polycode.util.Status
import polycode.util.TransactionData
import polycode.util.TransactionHash
import polycode.util.UtcDateTime
import polycode.util.WalletAddress
import polycode.util.WithTransactionData

data class Erc20LockRequest(
    val id: Erc20LockRequestId,
    val projectId: ProjectId,
    val chainId: ChainId,
    val redirectUrl: String,
    val tokenAddress: ContractAddress,
    val tokenAmount: Balance,
    val lockDuration: DurationSeconds,
    val lockContractAddress: ContractAddress,
    val tokenSenderAddress: WalletAddress?,
    val txHash: TransactionHash?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val createdAt: UtcDateTime
) {
    fun withTransactionData(
        status: Status,
        data: FunctionData,
        transactionInfo: BlockchainTransactionInfo?
    ): WithTransactionData<Erc20LockRequest> =
        WithTransactionData(
            value = this,
            status = status,
            transactionData = TransactionData(
                txHash = this.txHash,
                transactionInfo = transactionInfo,
                fromAddress = this.tokenSenderAddress,
                toAddress = this.lockContractAddress,
                data = data,
                value = null
            )
        )
}
