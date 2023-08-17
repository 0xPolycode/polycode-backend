package polycode.model.result

import polycode.util.Balance
import polycode.util.ContractAddress
import polycode.util.EthereumAddress
import polycode.util.FunctionData
import polycode.util.TransactionHash
import polycode.util.UtcDateTime
import polycode.util.WalletAddress
import java.math.BigInteger

data class BlockchainTransactionInfo(
    val hash: TransactionHash,
    val from: WalletAddress,
    val to: EthereumAddress,
    val deployedContractAddress: ContractAddress?,
    val data: FunctionData,
    val value: Balance,
    val blockConfirmations: BigInteger,
    val timestamp: UtcDateTime,
    val success: Boolean,
    val events: List<EventInfo>
) {
    fun hashMatches(expectedHash: TransactionHash?): Boolean =
        hash == expectedHash

    fun fromAddressOptionallyMatches(optionalAddress: WalletAddress?): Boolean =
        optionalAddress == null || from == optionalAddress

    fun toAddressMatches(toAddress: EthereumAddress): Boolean =
        to.toWalletAddress() == toAddress.toWalletAddress()

    fun deployedContractAddressIsNull(): Boolean =
        deployedContractAddress == null

    fun deployedContractAddressMatches(contractAddress: ContractAddress?): Boolean =
        deployedContractAddress != null && contractAddress == deployedContractAddress

    fun dataMatches(expectedData: FunctionData): Boolean =
        data == expectedData

    fun valueMatches(expectedValue: Balance): Boolean =
        value == expectedValue
}
