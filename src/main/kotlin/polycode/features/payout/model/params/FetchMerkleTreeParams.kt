package polycode.features.payout.model.params

import polycode.features.payout.util.MerkleHash
import polycode.util.ChainId
import polycode.util.ContractAddress

data class FetchMerkleTreeParams(
    val rootHash: MerkleHash,
    val chainId: ChainId,
    val assetContractAddress: ContractAddress
)
