package polycode.features.payout.model.response

import com.fasterxml.jackson.annotation.JsonIgnore
import polycode.features.payout.util.HashFunction
import polycode.features.payout.util.MerkleHash
import polycode.features.payout.util.MerkleTree
import polycode.util.annotation.SchemaAnyOf
import polycode.util.annotation.SchemaIgnore
import polycode.util.annotation.SchemaName
import polycode.util.annotation.SchemaNotNull
import java.math.BigInteger

private data class MerkleTreeSchema(
    val depth: Int,
    val hash: MerkleHash,
    val hashFn: HashFunction,
    val left: NodeSchema,
    val right: NodeSchema
)

@SchemaAnyOf
private data class NodeSchema(
    val node1: NilNodeSchema,
    val node2: LeafNodeSchema,
    val node3: PathNodeSchema
)

private data class NilNodeSchema(
    val hash: MerkleHash
)

private data class LeafNodeSchema(
    val hash: MerkleHash,
    val data: LeafNodeDataSchema
)

private data class LeafNodeDataSchema(
    val address: String,
    val balance: BigInteger
)

private data class PathNodeSchema(
    val hash: MerkleHash,
    val left: NodeSchema,
    val right: NodeSchema
)

data class FetchMerkleTreeResponse(
    @SchemaIgnore
    val merkleTree: MerkleTree
) {
    @Suppress("unused") // used for JSON schema generation
    @JsonIgnore
    @SchemaName("merkle_tree")
    @SchemaNotNull
    private val schemaMerkleTree: MerkleTreeSchema? = null
}
