package polycode.model.result

import polycode.util.Balance
import polycode.util.BlockNumber
import polycode.util.ContractAddress
import polycode.util.ContractBinaryData
import polycode.util.FunctionData
import polycode.util.TransactionHash
import polycode.util.WalletAddress

sealed interface ContractDeploymentTransactionInfo {
    val deployedContractAddress: ContractAddress
    val binary: ContractBinaryData
}

data class FullContractDeploymentTransactionInfo(
    val hash: TransactionHash,
    val from: WalletAddress,
    override val deployedContractAddress: ContractAddress,
    val data: FunctionData,
    val value: Balance,
    override val binary: ContractBinaryData,
    val blockNumber: BlockNumber,
    val events: List<EventInfo>
) : ContractDeploymentTransactionInfo

data class ContractBinaryInfo(
    override val deployedContractAddress: ContractAddress,
    override val binary: ContractBinaryData,
) : ContractDeploymentTransactionInfo
