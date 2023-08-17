package polycode.features.contract.arbitrarycall.model.result

import com.fasterxml.jackson.databind.JsonNode
import polycode.generated.jooq.id.ContractArbitraryCallRequestId
import polycode.generated.jooq.id.ContractDeploymentRequestId
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

data class ContractArbitraryCallRequest(
    val id: ContractArbitraryCallRequestId,
    val deployedContractId: ContractDeploymentRequestId?,
    val contractAddress: ContractAddress,
    val functionData: FunctionData,
    val functionName: String?,
    val functionParams: JsonNode?,
    val ethAmount: Balance,
    val chainId: ChainId,
    val redirectUrl: String,
    val projectId: ProjectId,
    val createdAt: UtcDateTime,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val callerAddress: WalletAddress?,
    val txHash: TransactionHash?
) {
    fun withTransactionData(
        status: Status,
        transactionInfo: BlockchainTransactionInfo?
    ): WithTransactionData<ContractArbitraryCallRequest> =
        WithTransactionData(
            value = this,
            status = status,
            transactionData = TransactionData(
                txHash = this.txHash,
                transactionInfo = transactionInfo,
                fromAddress = this.callerAddress,
                toAddress = this.contractAddress,
                data = this.functionData,
                value = ethAmount
            )
        )
}
