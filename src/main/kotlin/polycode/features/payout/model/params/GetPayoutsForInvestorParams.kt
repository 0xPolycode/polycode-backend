package polycode.features.payout.model.params

import polycode.util.ContractAddress
import polycode.util.WalletAddress

data class GetPayoutsForInvestorParams(
    val payoutManager: ContractAddress,
    val investor: WalletAddress
)
