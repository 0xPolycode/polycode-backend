package polycode.features.payout.model.response

import polycode.features.payout.util.MerkleTree.Companion.PathSegment
import java.math.BigInteger

data class InvestorPayoutResponse(
    val payout: PayoutResponse,
    val investor: String,
    val amountClaimed: BigInteger,

    val amountClaimable: BigInteger,
    val balance: BigInteger,
    val proof: List<String>
) {
    companion object {
        @Suppress("LongParameterList")
        operator fun invoke(
            payout: PayoutResponse,
            investor: String,
            amountClaimed: BigInteger,
            amountClaimable: BigInteger,
            balance: BigInteger,
            path: List<PathSegment>
        ) = InvestorPayoutResponse(
            payout = payout,
            investor = investor,
            amountClaimed = amountClaimed,
            amountClaimable = amountClaimable,
            balance = balance,
            proof = path.map { it.siblingHash.value }
        )
    }
}
