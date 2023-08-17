package polycode.features.contract.deployment.model.result

import com.fasterxml.jackson.databind.JsonNode
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.model.ScreenConfig
import polycode.model.result.BlockchainTransactionInfo
import polycode.util.Balance
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.ContractBinaryData
import polycode.util.ContractId
import polycode.util.ContractTag
import polycode.util.FunctionData
import polycode.util.InterfaceId
import polycode.util.Status
import polycode.util.TransactionData
import polycode.util.TransactionHash
import polycode.util.UtcDateTime
import polycode.util.WalletAddress
import polycode.util.WithTransactionData
import polycode.util.ZeroAddress

data class ContractDeploymentRequest(
    val id: ContractDeploymentRequestId,
    val alias: String,
    val name: String?,
    val description: String?,
    val contractId: ContractId,
    val contractData: ContractBinaryData,
    val constructorParams: JsonNode,
    val contractTags: List<ContractTag>,
    val contractImplements: List<InterfaceId>,
    val initialEthAmount: Balance,
    val chainId: ChainId,
    val redirectUrl: String,
    val projectId: ProjectId,
    val createdAt: UtcDateTime,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val contractAddress: ContractAddress?,
    val deployerAddress: WalletAddress?,
    val txHash: TransactionHash?,
    val imported: Boolean,
    val proxy: Boolean,
    val implementationContractAddress: ContractAddress?
) {
    fun withTransactionData(
        status: Status,
        transactionInfo: BlockchainTransactionInfo?
    ): WithTransactionData<ContractDeploymentRequest> =
        WithTransactionData(
            value = this,
            status = status,
            transactionData = TransactionData(
                txHash = this.txHash,
                transactionInfo = transactionInfo,
                fromAddress = this.deployerAddress,
                toAddress = ZeroAddress,
                data = FunctionData(contractData.value),
                value = initialEthAmount
            )
        )
}
