package polycode.features.payout.util.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import polycode.features.payout.util.MerkleHash

class MerkleHashJsonSerializer : JsonSerializer<MerkleHash>() {

    override fun serialize(hash: MerkleHash, json: JsonGenerator, provider: SerializerProvider) =
        json.writeString(hash.value)
}
