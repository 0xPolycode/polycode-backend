package polycode.util.json

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.web3j.abi.datatypes.Bytes
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.DynamicStruct
import org.web3j.abi.datatypes.StaticStruct
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.Utf8String
import polycode.features.functions.encoding.model.FunctionArgument
import polycode.util.SizedStaticArray

@Suppress("TooManyFunctions")
class FunctionArgumentJsonDeserializer : JsonDeserializer<FunctionArgument>() {

    companion object {
        private const val ARRAY_VALUE_ERROR = "invalid value type; expected array"
        private val ARRAY_REGEX_WITH_SIZE = "^(.+?)\\[(\\d*)]$".toRegex()
        private val ARRAY_SUFFIX_REGEX = "^(.+?)(\\[\\d*])$".toRegex()
        private val OBJECT_MAPPER = ObjectMapper()
    }

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): FunctionArgument {
        val jsonTree = p.readValueAsTree<JsonNode>()

        if (jsonTree !is ObjectNode) {
            throw JsonParseException(p, "object expected")
        }

        val argumentType = jsonTree["type"]?.asText() ?: throw JsonParseException(p, "missing type")
        val argumentValue = jsonTree["value"] ?: throw JsonParseException(p, "missing value")

        return FunctionArgument(deserializeType(p, argumentType, argumentValue), jsonTree)
    }

    private fun deserializeType(p: JsonParser, argumentType: String, argumentValue: JsonNode): Type<*> {
        val arrayMatchingResult = ARRAY_REGEX_WITH_SIZE.find(argumentType)

        return if (arrayMatchingResult != null) {
            val (_, arrayElementType, arraySize) = arrayMatchingResult.groupValues
            argumentValue.parseArray(p, arrayElementType, arraySize.toIntOrNull())
        } else if (argumentType == "tuple") {
            argumentValue.parseTuple(p)
        } else {
            Web3TypeMappings[argumentType]
                ?.let { it(argumentValue, p) }
                ?: throw JsonParseException(p, "unknown type: $argumentType")
        }
    }

    private fun JsonNode.parseArray(p: JsonParser, elementType: String, length: Int?): Type<*> =
        if (this.isArray) {
            this.elements().asSequence().map { Pair(it, deserializeType(p, elementType, it)) }.toList().takeIf {
                length == null || it.size == length
            }
                ?.checkTupleCompatibility(p, elementType)
                ?.createArray(elementType.web3ElementType(p, this), length)
                ?: throw JsonParseException(p, "invalid array length")
        } else {
            throw JsonParseException(p, ARRAY_VALUE_ERROR)
        }

    @Suppress("UNCHECKED_CAST")
    private fun List<Type<*>>.createArray(web3ElementType: Class<out Type<*>>, length: Int?): Type<*> {
        val web3FixedStructElementType = web3ElementType.fixStructType(this)

        return if (length == null) {
            DynamicArray(web3FixedStructElementType as Class<Type<*>>, this)
        } else {
            SizedStaticArray(web3FixedStructElementType as Class<Type<*>>, this)
        }
    }

    private fun Class<out Type<*>>.fixStructType(elems: List<Type<*>>): Class<out Type<*>> =
        elems.firstOrNull()?.javaClass?.takeIf { it == DynamicStruct::class.java || it == StaticStruct::class.java }
            ?: this

    private fun List<Pair<JsonNode, Type<*>>>.checkTupleCompatibility(
        p: JsonParser,
        elementType: String
    ): List<Type<*>> {
        if (elementType == "tuple" && this.hasInvalidTupleTypeHierarchy()) {
            throw JsonParseException(p, "mismatching tuple elements in array")
        }

        return this.map { it.second }
    }

    private fun String.web3ElementType(p: JsonParser, node: JsonNode): Class<out Type<*>> =
        if (this.endsWith("[]")) {
            DynamicArray::class.java
        } else if (ARRAY_REGEX_WITH_SIZE.matches(this)) {
            SizedStaticArray::class.java
        } else if (this == "tuple") {
            DynamicStruct::class.java
        } else {
            Web3TypeMappings.getWeb3Type(p, node, this)
                ?: throw JsonParseException(p, "unknown type: $this")
        }

    private fun JsonNode.parseTuple(p: JsonParser): Type<*> =
        if (this.isArray) {
            val tupleElements = this.elements().asSequence().map {
                val tupleArgumentType = it["type"]?.asText() ?: throw JsonParseException(p, "missing type")
                val tupleArgumentValue = it["value"] ?: throw JsonParseException(p, "missing value")
                deserializeType(p, tupleArgumentType, tupleArgumentValue)
            }.toList().takeIf { it.isNotEmpty() } ?: throw JsonParseException(p, "tuples cannot be empty")

            if (tupleElements.any { it.isDynamic() }) DynamicStruct(tupleElements) else StaticStruct(tupleElements)
        } else {
            throw JsonParseException(p, ARRAY_VALUE_ERROR)
        }

    private fun List<Pair<JsonNode, Type<*>>>.hasInvalidTupleTypeHierarchy() =
        this.map {
            getTypeHierarchy(
                OBJECT_MAPPER.createObjectNode().apply {
                    set<JsonNode>("value", it.first)
                    set<JsonNode>("type", textNode("tuple"))
                }
            )
        }.toSet().size > 1

    internal fun getTypeHierarchy(node: JsonNode): String {
        val type = node["type"].asText()
        val arrayMatchingResult = ARRAY_SUFFIX_REGEX.find(type)

        return if (arrayMatchingResult != null) {
            val (_, arrayElementType, arraySuffix) = arrayMatchingResult.groupValues
            val arrayNode = OBJECT_MAPPER.createObjectNode().apply {
                set<JsonNode>(
                    "value",
                    node["value"].elements().asSequence().toList().getOrNull(0) ?: OBJECT_MAPPER.createArrayNode()
                )
                set<JsonNode>("type", textNode(arrayElementType))
            }

            getTypeHierarchy(arrayNode) + arraySuffix
        } else if (type == "tuple") {
            node["value"].elements().asSequence().map { getTypeHierarchy(it) }
                .ifEmpty { sequenceOf("*") }
                .joinToString(prefix = "tuple(", separator = ",", postfix = ")")
        } else {
            type
        }
    }

    private fun Type<*>.isDynamic() =
        when (this) {
            is Utf8String -> true
            is DynamicStruct -> true
            is DynamicArray<*> -> true
            else -> this == Bytes::class.java
        }
}
