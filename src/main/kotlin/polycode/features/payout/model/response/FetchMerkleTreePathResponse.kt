package polycode.features.payout.model.response

import com.fasterxml.jackson.annotation.JsonProperty
import polycode.features.payout.util.MerkleTree.Companion.PathSegment
import java.math.BigInteger

data class FetchMerkleTreePathResponse(
    val walletAddress: String,
    val walletBalance: BigInteger,
    val path: List<PathSegment>
) {
    @Suppress("unused") // returned in JSON
    @JsonProperty
    private val proof: List<String> = path.map { it.siblingHash.value }
}
