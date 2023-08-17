package polycode.features.payout.model.params

import polycode.generated.jooq.id.ProjectId
import polycode.util.BlockNumber
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.WalletAddress

data class CreateAssetSnapshotParams(
    val name: String,
    val chainId: ChainId,
    val projectId: ProjectId,
    val assetContractAddress: ContractAddress,
    val payoutBlock: BlockNumber,
    val ignoredHolderAddresses: Set<WalletAddress>
)
