package polycode.features.payout.util

import polycode.features.payout.util.recursion.FlatMap
import polycode.features.payout.util.recursion.Return
import polycode.features.payout.util.recursion.Suspend
import polycode.features.payout.util.recursion.Trampoline
import polycode.util.WalletAddress
import java.util.LinkedList
import java.util.SortedMap

class MerkleTree(nodes: List<PayoutAccountBalance>, val hashFn: HashFunction) {

    companion object {
        sealed interface Node {
            val hash: MerkleHash
        }

        sealed interface PathNode : Node {
            val left: Node
            val right: Node
        }

        object NilNode : Node {
            override val hash: MerkleHash =
                MerkleHash("0x0000000000000000000000000000000000000000000000000000000000000000")
        }

        data class LeafNode(val data: PayoutAccountBalance, override val hash: MerkleHash) : Node
        data class MiddleNode(
            override val left: Node,
            override val right: Node,
            override val hash: MerkleHash
        ) : PathNode

        data class RootNode(
            override val left: Node,
            override val right: Node,
            override val hash: MerkleHash,
            val depth: Int
        ) : PathNode

        data class PathSegment(val siblingHash: MerkleHash, val isLeft: Boolean)
    }

    val leafNodesByHash: Map<MerkleHash, IndexedValue<LeafNode>>
    val leafNodesByAddress: Map<WalletAddress, IndexedValue<LeafNode>>
    val root: RootNode

    init {
        require(nodes.isNotEmpty()) { "Cannot build Merkle tree from empty list" }

        val byAddress: Map<WalletAddress, LeafNode> = nodes.map { LeafNode(it, it.hash) }
            .groupBy { it.data.address }
            .mapValues {
                require(it.value.size == 1) { "Address collision while constructing leaf nodes: ${it.key}" }
                it.value.first()
            }
        val bySortedHash: SortedMap<MerkleHash, LeafNode> = byAddress.values
            .groupBy { it.hash }
            .mapValues {
                require(it.value.size == 1) { "Hash collision while constructing leaf nodes: ${it.key}" }
                it.value.first()
            }.toSortedMap()

        root = buildTree(bySortedHash.values.toList())

        val indexedLeafNodes = indexLeafNodes()

        leafNodesByHash = indexedLeafNodes.associateBy { it.value.hash }
        leafNodesByAddress = indexedLeafNodes.associateBy { it.value.data.address }
    }

    fun pathTo(element: PayoutAccountBalance): List<PathSegment>? {
        val index = leafNodesByHash[element.hash]?.index ?: return null
        val moves = index.toString(2).padStart(root.depth, '0')

        tailrec fun findPath(currentNode: Node, d: Int, path: LinkedList<PathSegment>): List<PathSegment> {
            return if (currentNode is PathNode) {
                val isLeft = moves[d] == '0'
                val nextNode = if (isLeft) currentNode.left else currentNode.right
                val siblingNode = if (isLeft.not()) currentNode.left else currentNode.right
                findPath(nextNode, d + 1, path.withFirst(PathSegment(siblingNode.hash, isLeft.not())))
            } else {
                path
            }
        }

        return findPath(root, 0, LinkedList())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (other !is MerkleTree) {
            return false
        }

        return other.root == root
    }

    override fun hashCode(): Int {
        return root.hashCode()
    }

    private fun buildTree(leafNodes: List<LeafNode>): RootNode {
        tailrec fun buildLayer(nodes: Collection<Node>, depth: Int): RootNode {
            val pairs = nodes.pairwise()

            return if (pairs.size == 1) {
                val pair = pairs[0]
                RootNode(pair.left, pair.right, pair.hash, depth)
            } else {
                val parentLayer = pairs.map { MiddleNode(it.left, it.right, it.hash) }
                buildLayer(parentLayer, depth + 1)
            }
        }

        return buildLayer(leafNodes, 1)
    }

    private fun indexLeafNodes(): List<IndexedValue<LeafNode>> {

        fun indexPath(currentNode: Node, currentIndex: String): Trampoline<List<IndexedValue<LeafNode>>> {
            return when (currentNode) {
                is PathNode -> {
                    val left = Suspend { indexPath(currentNode.left, currentIndex + "0") }
                    val right = Suspend { indexPath(currentNode.right, currentIndex + "1") }

                    FlatMap(left) { leftList ->
                        FlatMap(right) { rightList ->
                            Return(leftList + rightList)
                        }
                    }
                }

                is LeafNode -> {
                    Return(listOf(IndexedValue(currentIndex.toInt(2), currentNode)))
                }

                else -> {
                    Return(emptyList())
                }
            }
        }

        return Trampoline.run(indexPath(root, "0"))
    }

    private val PayoutAccountBalance.hash: MerkleHash
        get() = hashFn(abiEncode())

    private val Pair<Node, Node>.left: Node
        get() = if (first.hash <= second.hash) first else second

    private val Pair<Node, Node>.right: Node
        get() = if (first.hash <= second.hash) second else first

    private val Pair<Node, Node>.hash: MerkleHash
        get() = hashFn((left.hash + right.hash).value)

    private fun Collection<Node>.pairwise(): List<Pair<Node, Node>> =
        this.chunked(2).map { Pair(it.first(), it.getOrNull(1) ?: NilNode) }

    private fun LinkedList<PathSegment>.withFirst(first: PathSegment): LinkedList<PathSegment> {
        addFirst(first)
        return this
    }
}
