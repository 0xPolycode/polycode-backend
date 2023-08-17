package polycode.features.payout.service

import polycode.features.payout.model.params.CreateAssetSnapshotParams
import polycode.features.payout.model.result.FullAssetSnapshot
import polycode.features.payout.util.AssetSnapshotStatus
import polycode.generated.jooq.id.AssetSnapshotId
import polycode.generated.jooq.id.ProjectId

interface AssetSnapshotQueueService {
    fun submitAssetSnapshot(params: CreateAssetSnapshotParams): AssetSnapshotId
    fun getAssetSnapshotById(assetSnapshotId: AssetSnapshotId): FullAssetSnapshot?

    fun getAllAssetSnapshotsByProjectIdAndStatuses(
        projectId: ProjectId,
        statuses: Set<AssetSnapshotStatus>
    ): List<FullAssetSnapshot>
}
