package polycode.features.payout.model.result

import org.web3j.utils.Numeric
import polycode.blockchain.PayoutStruct
import polycode.features.payout.model.response.PayoutResponse
import polycode.features.payout.util.IpfsHash
import polycode.features.payout.util.MerkleHash
import polycode.util.Balance
import polycode.util.BlockNumber
import polycode.util.ContractAddress
import polycode.util.WalletAddress
import java.math.BigInteger

data class Payout(
    val payoutId: BigInteger,
    val payoutOwner: WalletAddress,
    val payoutInfo: String,
    val isCanceled: Boolean,
    val asset: ContractAddress,
    val totalAssetAmount: Balance,
    val ignoredHolderAddresses: Set<WalletAddress>,
    val assetSnapshotMerkleRoot: MerkleHash,
    val assetSnapshotMerkleDepth: BigInteger,
    val assetSnapshotBlockNumber: BlockNumber,
    val assetSnapshotMerkleIpfsHash: IpfsHash,
    val rewardAsset: ContractAddress,
    val totalRewardAmount: Balance,
    val remainingRewardAmount: Balance
) {
    constructor(struct: PayoutStruct) : this(
        payoutId = struct.payoutId,
        payoutOwner = WalletAddress(struct.payoutOwner),
        payoutInfo = struct.payoutInfo,
        isCanceled = struct.isCanceled,
        asset = ContractAddress(struct.asset),
        totalAssetAmount = Balance(struct.totalAssetAmount),
        ignoredHolderAddresses = struct.ignoredHolderAddresses.mapTo(HashSet()) { WalletAddress(it) },
        assetSnapshotMerkleRoot = MerkleHash(Numeric.toHexString(struct.assetSnapshotMerkleRoot)),
        assetSnapshotMerkleDepth = struct.assetSnapshotMerkleDepth,
        assetSnapshotBlockNumber = BlockNumber(struct.assetSnapshotBlockNumber),
        assetSnapshotMerkleIpfsHash = IpfsHash(struct.assetSnapshotMerkleIpfsHash),
        rewardAsset = ContractAddress(struct.rewardAsset),
        totalRewardAmount = Balance(struct.totalRewardAmount),
        remainingRewardAmount = Balance(struct.remainingRewardAmount)
    )

    fun toPayoutResponse(): PayoutResponse =
        PayoutResponse(
            payoutId = payoutId,
            payoutOwner = payoutOwner.rawValue,
            payoutInfo = payoutInfo,
            isCanceled = isCanceled,

            asset = asset.rawValue,
            totalAssetAmount = totalAssetAmount.rawValue,
            ignoredHolderAddresses = ignoredHolderAddresses.mapTo(HashSet()) { it.rawValue },

            assetSnapshotMerkleRoot = assetSnapshotMerkleRoot.value,
            assetSnapshotMerkleDepth = assetSnapshotMerkleDepth.intValueExact(),
            assetSnapshotBlockNumber = assetSnapshotBlockNumber.value,
            assetSnapshotMerkleIpfsHash = assetSnapshotMerkleIpfsHash.value,

            rewardAsset = rewardAsset.rawValue,
            totalRewardAmount = totalRewardAmount.rawValue,
            remainingRewardAmount = remainingRewardAmount.rawValue
        )
}
