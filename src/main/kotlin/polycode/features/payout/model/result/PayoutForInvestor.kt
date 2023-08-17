package polycode.features.payout.model.result

import polycode.blockchain.PayoutStateForInvestor
import polycode.blockchain.PayoutStruct
import polycode.util.Balance
import polycode.util.WalletAddress

data class PayoutForInvestor(
    val payout: Payout,
    val investor: WalletAddress,
    val amountClaimed: Balance
) {
    constructor(struct: PayoutStruct, state: PayoutStateForInvestor) : this(
        payout = Payout(struct),
        investor = WalletAddress(state.investor),
        amountClaimed = Balance(state.amountClaimed)
    )
}
