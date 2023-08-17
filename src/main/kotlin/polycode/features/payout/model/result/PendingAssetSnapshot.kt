package polycode.features.payout.model.result

import polycode.generated.jooq.id.AssetSnapshotId
import polycode.generated.jooq.id.ProjectId
import polycode.util.BlockNumber
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.WalletAddress

data class PendingAssetSnapshot(
    val id: AssetSnapshotId,
    val projectId: ProjectId,
    val name: String,
    val chainId: ChainId,
    val assetContractAddress: ContractAddress,
    val blockNumber: BlockNumber,
    val ignoredHolderAddresses: Set<WalletAddress>
)
