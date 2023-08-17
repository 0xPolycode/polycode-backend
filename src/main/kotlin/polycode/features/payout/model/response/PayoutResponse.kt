package polycode.features.payout.model.response

import java.math.BigInteger

data class PayoutResponse(
    val payoutId: BigInteger,
    val payoutOwner: String,
    val payoutInfo: String,
    val isCanceled: Boolean,

    val asset: String,
    val totalAssetAmount: BigInteger,
    val ignoredHolderAddresses: Set<String>,

    val assetSnapshotMerkleRoot: String,
    val assetSnapshotMerkleDepth: Int,
    val assetSnapshotBlockNumber: BigInteger,
    val assetSnapshotMerkleIpfsHash: String,

    val rewardAsset: String,
    val totalRewardAmount: BigInteger,
    val remainingRewardAmount: BigInteger
)
