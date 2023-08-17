package polycode.features.payout.repository

import polycode.features.payout.model.params.CreateAssetSnapshotParams
import polycode.features.payout.model.result.AssetSnapshot
import polycode.features.payout.model.result.PendingAssetSnapshot
import polycode.features.payout.util.AssetSnapshotFailureCause
import polycode.features.payout.util.AssetSnapshotStatus
import polycode.features.payout.util.IpfsHash
import polycode.generated.jooq.id.AssetSnapshotId
import polycode.generated.jooq.id.MerkleTreeRootId
import polycode.generated.jooq.id.ProjectId
import polycode.util.Balance

interface AssetSnapshotRepository {
    fun getById(assetSnapshotId: AssetSnapshotId): AssetSnapshot?

    fun getAllByProjectIdAndStatuses(
        projectId: ProjectId,
        statuses: Set<AssetSnapshotStatus>
    ): List<AssetSnapshot>

    fun createAssetSnapshot(params: CreateAssetSnapshotParams): AssetSnapshotId
    fun getPending(): PendingAssetSnapshot?

    fun completeAssetSnapshot(
        assetSnapshotId: AssetSnapshotId,
        merkleTreeRootId: MerkleTreeRootId,
        merkleTreeIpfsHash: IpfsHash,
        totalAssetAmount: Balance
    ): AssetSnapshot?

    fun failAssetSnapshot(assetSnapshotId: AssetSnapshotId, cause: AssetSnapshotFailureCause): AssetSnapshot?
}
