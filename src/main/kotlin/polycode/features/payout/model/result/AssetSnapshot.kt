package polycode.features.payout.model.result

import polycode.features.payout.util.AssetSnapshotFailureCause
import polycode.features.payout.util.AssetSnapshotStatus
import polycode.features.payout.util.IpfsHash
import polycode.generated.jooq.id.AssetSnapshotId
import polycode.generated.jooq.id.MerkleTreeRootId
import polycode.generated.jooq.id.ProjectId
import polycode.util.Balance
import polycode.util.BlockNumber
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.WalletAddress

data class AssetSnapshot(
    val id: AssetSnapshotId,
    val projectId: ProjectId,
    val name: String,
    val chainId: ChainId,
    val assetContractAddress: ContractAddress,
    val blockNumber: BlockNumber,
    val ignoredHolderAddresses: Set<WalletAddress>,
    val data: OptionalAssetSnapshotData
)

sealed interface OptionalAssetSnapshotData {
    val status: AssetSnapshotStatus
    val failureCause: AssetSnapshotFailureCause?
}

data class SuccessfulAssetSnapshotData(
    val merkleTreeRootId: MerkleTreeRootId,
    val merkleTreeIpfsHash: IpfsHash,
    val totalAssetAmount: Balance,
    override val status: AssetSnapshotStatus = AssetSnapshotStatus.SUCCESS,
    override val failureCause: AssetSnapshotFailureCause? = null
) : OptionalAssetSnapshotData

data class OtherAssetSnapshotData(
    override val status: AssetSnapshotStatus,
    override val failureCause: AssetSnapshotFailureCause?
) : OptionalAssetSnapshotData
