package polycode.features.payout.util.json

import com.fasterxml.jackson.databind.JsonNode
import org.junit.jupiter.api.Test
import polycode.TestBase
import polycode.config.JsonConfig
import polycode.features.payout.util.MerkleHash
import polycode.features.payout.util.MerkleTree.Companion.PathSegment

class PathSegmentJsonSerializerTest : TestBase() {

    private val objectMapper = JsonConfig().objectMapper()

    @Test
    fun mustCorrectlySerializePathSegment() {
        val pathSegment = PathSegment(MerkleHash("test"), true)

        val serializedPathSegment = suppose("path segment is serialized to JSON") {
            objectMapper.valueToTree<JsonNode>(pathSegment)
        }

        verify("path segment is correctly serialized") {
            expectThat(serializedPathSegment).isEqualTo(
                objectMapper.readTree(
                    """
                    {
                        "sibling_hash": "${pathSegment.siblingHash.value}",
                        "is_left": ${pathSegment.isLeft}
                    }
                    """.trimIndent()
                )
            )
        }
    }
}
