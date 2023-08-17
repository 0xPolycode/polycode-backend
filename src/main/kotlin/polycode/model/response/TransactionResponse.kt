package polycode.model.response

import polycode.util.Balance
import polycode.util.EthereumAddress
import polycode.util.FunctionData
import polycode.util.TransactionData
import polycode.util.WalletAddress
import java.math.BigInteger
import java.time.OffsetDateTime

data class TransactionResponse(
    val txHash: String?,
    val from: String?,
    val to: String,
    val data: String?,
    val value: BigInteger,
    val blockConfirmations: BigInteger?,
    val timestamp: OffsetDateTime?
) {
    companion object {
        fun unmined(from: WalletAddress?, to: EthereumAddress, data: FunctionData?, value: Balance) =
            TransactionResponse(
                txHash = null,
                from = from?.rawValue,
                to = to.rawValue,
                data = data?.nullIfEmpty()?.value,
                value = value.rawValue,
                blockConfirmations = null,
                timestamp = null
            )

        private fun FunctionData.nullIfEmpty(): FunctionData? =
            if (this == FunctionData.EMPTY) null else this
    }

    constructor(transactionData: TransactionData) : this(
        txHash = transactionData.txHash?.value,
        from = transactionData.fromAddress?.rawValue,
        to = transactionData.toAddress.rawValue,
        data = transactionData.data?.nullIfEmpty()?.value,
        value = transactionData.value.rawValue,
        blockConfirmations = transactionData.blockConfirmations,
        timestamp = transactionData.timestamp?.value
    )
}
