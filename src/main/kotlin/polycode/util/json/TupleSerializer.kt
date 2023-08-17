package polycode.util.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import polycode.features.contract.abi.model.Tuple

class TupleSerializer : JsonSerializer<Tuple>() {
    override fun serialize(value: Tuple, gen: JsonGenerator, serializers: SerializerProvider) {
        serializers.findValueSerializer(List::class.java).serialize(value.elems, gen, serializers)
    }
}
