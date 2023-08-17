package polycode.features.payout.model.result

import polycode.features.payout.util.MerkleTree
import polycode.generated.jooq.id.MerkleTreeRootId

data class MerkleTreeWithId(val treeId: MerkleTreeRootId, val tree: MerkleTree)
