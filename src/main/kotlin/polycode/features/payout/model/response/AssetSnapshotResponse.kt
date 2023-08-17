package polycode.features.payout.model.response

import polycode.features.payout.util.AssetSnapshotFailureCause
import polycode.features.payout.util.AssetSnapshotStatus
import polycode.generated.jooq.id.AssetSnapshotId
import polycode.generated.jooq.id.ProjectId
import java.math.BigInteger

data class AssetSnapshotResponse(
    val id: AssetSnapshotId,
    val projectId: ProjectId,
    val name: String,
    val chainId: Long,
    val status: AssetSnapshotStatus,
    val failureCause: AssetSnapshotFailureCause?,
    val asset: String,
    val totalAssetAmount: BigInteger?,
    val ignoredHolderAddresses: Set<String>,
    val assetSnapshotMerkleRoot: String?,
    val assetSnapshotMerkleDepth: Int?,
    val assetSnapshotBlockNumber: BigInteger,
    val assetSnapshotMerkleIpfsHash: String?
)
