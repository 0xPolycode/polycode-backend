package polycode.features.payout.model.result

import polycode.features.payout.model.response.AssetSnapshotResponse
import polycode.features.payout.util.AssetSnapshotFailureCause
import polycode.features.payout.util.AssetSnapshotStatus
import polycode.features.payout.util.HashFunction
import polycode.features.payout.util.IpfsHash
import polycode.features.payout.util.MerkleHash
import polycode.generated.jooq.id.AssetSnapshotId
import polycode.generated.jooq.id.ProjectId
import polycode.util.Balance
import polycode.util.BlockNumber
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.WalletAddress

data class FullAssetSnapshot(
    val id: AssetSnapshotId,
    val projectId: ProjectId,
    val name: String,
    val chainId: ChainId,
    val assetContractAddress: ContractAddress,
    val blockNumber: BlockNumber,
    val ignoredHolderAddresses: Set<WalletAddress>,
    val snapshotStatus: AssetSnapshotStatus,
    val snapshotFailureCause: AssetSnapshotFailureCause?,
    val data: FullAssetSnapshotData?
) {
    fun toAssetSnapshotResponse(): AssetSnapshotResponse =
        AssetSnapshotResponse(
            id = id,
            projectId = projectId,
            name = name,
            chainId = chainId.value,
            status = snapshotStatus,
            failureCause = snapshotFailureCause,
            asset = assetContractAddress.rawValue,
            totalAssetAmount = data?.totalAssetAmount?.rawValue,
            ignoredHolderAddresses = ignoredHolderAddresses.mapTo(HashSet()) { it.rawValue },
            assetSnapshotMerkleRoot = data?.merkleRootHash?.value,
            assetSnapshotMerkleDepth = data?.merkleTreeDepth,
            assetSnapshotBlockNumber = blockNumber.value,
            assetSnapshotMerkleIpfsHash = data?.merkleTreeIpfsHash?.value
        )
}

data class FullAssetSnapshotData(
    val totalAssetAmount: Balance,
    val merkleRootHash: MerkleHash,
    val merkleTreeIpfsHash: IpfsHash,
    val merkleTreeDepth: Int,
    val hashFn: HashFunction
)
