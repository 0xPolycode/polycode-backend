package polycode.features.payout.util.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import polycode.features.payout.util.MerkleTree
import polycode.features.payout.util.MerkleTree.Companion.LeafNode
import polycode.features.payout.util.MerkleTree.Companion.NilNode
import polycode.features.payout.util.MerkleTree.Companion.Node
import polycode.features.payout.util.MerkleTree.Companion.PathNode

class MerkleTreeJsonSerializer : JsonSerializer<MerkleTree>() {

    override fun serialize(value: MerkleTree, json: JsonGenerator, provider: SerializerProvider) {
        json.apply {
            writeStartObject()

            writeNumberField("depth", value.root.depth)
            writeStringField("hash", value.root.hash.value)
            writeStringField("hash_fn", value.hashFn.name)

            writeBranch("left", value.root.left)
            writeBranch("right", value.root.right)

            writeEndObject()
        }
    }

    private fun JsonGenerator.writeBranch(fieldName: String, branch: Node) {
        writeObjectFieldStart(fieldName)
        writeStringField("hash", branch.hash.value)

        when (branch) {
            is NilNode -> {}
            is LeafNode -> {
                writeObjectFieldStart("data")
                writeStringField("address", branch.data.address.rawValue)
                writeStringField("balance", branch.data.balance.rawValue.toString())
                writeEndObject()
            }

            is PathNode -> {
                writeBranch("left", branch.left)
                writeBranch("right", branch.right)
            }
        }

        writeEndObject()
    }
}
