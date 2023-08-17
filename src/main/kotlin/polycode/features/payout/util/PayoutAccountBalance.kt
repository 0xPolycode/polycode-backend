package polycode.features.payout.util

import org.web3j.abi.TypeEncoder
import polycode.util.Balance
import polycode.util.WalletAddress

data class PayoutAccountBalance(val address: WalletAddress, val balance: Balance) {
    fun abiEncode(): String = TypeEncoder.encode(address.value) + TypeEncoder.encode(balance.value)
}
