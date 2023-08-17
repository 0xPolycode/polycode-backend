package polycode.features.payout.repository

import polycode.features.payout.model.params.FetchMerkleTreeParams
import polycode.features.payout.model.params.FetchMerkleTreePathParams
import polycode.features.payout.model.result.MerkleTreeWithId
import polycode.features.payout.util.MerkleTree
import polycode.generated.jooq.id.MerkleTreeRootId
import polycode.util.BlockNumber
import polycode.util.ChainId
import polycode.util.ContractAddress

interface MerkleTreeRepository {
    fun getById(treeId: MerkleTreeRootId): MerkleTree?
    fun storeTree(
        tree: MerkleTree,
        chainId: ChainId,
        assetContractAddress: ContractAddress,
        blockNumber: BlockNumber
    ): MerkleTreeRootId

    fun fetchTree(params: FetchMerkleTreeParams): MerkleTreeWithId?
    fun containsAddress(params: FetchMerkleTreePathParams): Boolean
}
