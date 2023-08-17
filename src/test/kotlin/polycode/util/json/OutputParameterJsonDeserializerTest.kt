package polycode.util.json

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import org.junit.jupiter.api.Test
import polycode.TestBase
import polycode.config.JsonConfig
import polycode.features.contract.abi.model.AddressType
import polycode.features.contract.abi.model.BoolType
import polycode.features.contract.abi.model.DynamicArrayType
import polycode.features.contract.abi.model.DynamicBytesType
import polycode.features.contract.abi.model.IntType
import polycode.features.contract.abi.model.StaticArrayType
import polycode.features.contract.abi.model.StaticBytesType
import polycode.features.contract.abi.model.StringType
import polycode.features.contract.abi.model.TupleType
import polycode.features.contract.abi.model.UintType
import polycode.features.contract.readcall.model.params.OutputParameter

class OutputParameterJsonDeserializerTest : TestBase() {

    companion object {
        private data class Result(val args: List<OutputParameter>)
    }

    private val objectMapper = JsonConfig().objectMapper()

    @Test
    fun mustCorrectlyDeserializeBaseTypes() {
        val json =
            """{
              |  "args": [
              |    "address", "bool", "string", "bytes", "byte", "uint", "uint8", "uint16", "uint24", "uint32",
              |    "uint40", "uint48", "uint56", "uint64", "uint72", "uint80", "uint88", "uint96", "uint104", "uint112",
              |    "uint120", "uint128", "uint136", "uint144", "uint152", "uint160", "uint168", "uint176", "uint184",
              |    "uint192", "uint200", "uint208", "uint216", "uint224", "uint232", "uint240", "uint248", "uint256",
              |    "int", "int8", "int16", "int24", "int32", "int40", "int48", "int56", "int64", "int72", "int80",
              |    "int88", "int96", "int104", "int112", "int120", "int128", "int136", "int144", "int152", "int160",
              |    "int168", "int176", "int184", "int192", "int200", "int208", "int216", "int224", "int232", "int240",
              |    "int248", "int256", "bytes1", "bytes2", "bytes3", "bytes4", "bytes5", "bytes6", "bytes7", "bytes8",
              |    "bytes9", "bytes10", "bytes11", "bytes12", "bytes13", "bytes14", "bytes15", "bytes16", "bytes17",
              |    "bytes18", "bytes19", "bytes20", "bytes21", "bytes22", "bytes23", "bytes24", "bytes25", "bytes26",
              |    "bytes27", "bytes28", "bytes29", "bytes30", "bytes31", "bytes32"
              |  ]
              |}
            """.trimMargin()

        verify("must correctly parse base types") {
            val result = objectMapper.readValue(json, Result::class.java).args.map { it.deserializedType }

            expectThat(result)
                .isEqualTo(
                    listOf(
                        AddressType, BoolType, StringType, DynamicBytesType, UintType, UintType, UintType, UintType,
                        UintType, UintType, UintType, UintType, UintType, UintType, UintType, UintType, UintType,
                        UintType, UintType, UintType, UintType, UintType, UintType, UintType, UintType, UintType,
                        UintType, UintType, UintType, UintType, UintType, UintType, UintType, UintType, UintType,
                        UintType, UintType, UintType, IntType, IntType, IntType, IntType, IntType, IntType, IntType,
                        IntType, IntType, IntType, IntType, IntType, IntType, IntType, IntType, IntType, IntType,
                        IntType, IntType, IntType, IntType, IntType, IntType, IntType, IntType, IntType, IntType,
                        IntType, IntType, IntType, IntType, IntType, IntType, StaticBytesType(1), StaticBytesType(2),
                        StaticBytesType(3), StaticBytesType(4), StaticBytesType(5), StaticBytesType(6),
                        StaticBytesType(7), StaticBytesType(8), StaticBytesType(9), StaticBytesType(10),
                        StaticBytesType(11), StaticBytesType(12), StaticBytesType(13), StaticBytesType(14),
                        StaticBytesType(15), StaticBytesType(16), StaticBytesType(17), StaticBytesType(18),
                        StaticBytesType(19), StaticBytesType(20), StaticBytesType(21), StaticBytesType(22),
                        StaticBytesType(23), StaticBytesType(24), StaticBytesType(25), StaticBytesType(26),
                        StaticBytesType(27), StaticBytesType(28), StaticBytesType(29), StaticBytesType(30),
                        StaticBytesType(31), StaticBytesType(32)
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyDeserializeDynamicArrayType() {
        val json =
            """{
              |  "args": ["string[]"]
              |}
            """.trimMargin()

        verify("must correctly parse dynamic array type") {
            val result = objectMapper.readValue(json, Result::class.java).args.map { it.deserializedType }

            expectThat(result)
                .isEqualTo(listOf(DynamicArrayType(StringType)))
        }
    }

    @Test
    fun mustCorrectlyDeserializeSizedArrayType() {
        val json =
            """{
              |  "args": ["bool[2]"]
              |}
            """.trimMargin()

        verify("must correctly parse sized array type") {
            val result = objectMapper.readValue(json, Result::class.java).args.map { it.deserializedType }

            expectThat(result)
                .isEqualTo(listOf(StaticArrayType(BoolType, 2)))
        }
    }

    @Test
    fun mustCorrectlyDeserializeNestedArrayType() {
        val json =
            """{
              |  "args": ["uint[][2][3]"]
              |}
            """.trimMargin()

        verify("must correctly parse nested array type") {
            val result = objectMapper.readValue(json, Result::class.java).args.map { it.deserializedType }

            expectThat(result)
                .isEqualTo(listOf(StaticArrayType(StaticArrayType(DynamicArrayType(UintType), 2), 3)))
        }
    }

    @Test
    fun mustCorrectlyDeserializeTuple() {
        val json =
            """{
              |  "args": [
              |    {
              |      "type": "tuple",
              |      "elems": ["string", "uint"]
              |    }
              |  ]
              |}
            """.trimMargin()

        verify("must correctly parse tuple") {
            val result = objectMapper.readValue(json, Result::class.java).args.map { it.deserializedType }

            expectThat(result)
                .isEqualTo(listOf(TupleType(StringType, UintType)))
        }
    }

    @Test
    fun mustCorrectlyDeserializeNestedTuple() {
        val json =
            """{
              |  "args": [
              |    {
              |      "type": "tuple",
              |      "elems": [
              |        "string",
              |        {
              |          "type": "tuple",
              |          "elems": [
              |            "string",
              |            {
              |              "type": "tuple",
              |              "elems": ["string", "bool", "bytes5"]
              |            },
              |            "int"
              |          ]
              |        },
              |        "uint"
              |      ]
              |    }
              |  ]
              |}
            """.trimMargin()

        verify("must correctly parse nested tuple") {
            val result = objectMapper.readValue(json, Result::class.java).args.map { it.deserializedType }

            expectThat(result)
                .isEqualTo(
                    listOf(
                        TupleType(
                            StringType,
                            TupleType(
                                StringType,
                                TupleType(StringType, BoolType, StaticBytesType(5)),
                                IntType
                            ),
                            UintType
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyDeserializeTupleWithArrayElements() {
        val json =
            """{
              |  "args": [
              |    {
              |      "type": "tuple",
              |      "elems": ["string[]", "uint[]"]
              |    }
              |  ]
              |}
            """.trimMargin()

        verify("must correctly parse tuple with array elements") {
            val result = objectMapper.readValue(json, Result::class.java).args.map { it.deserializedType }

            expectThat(result)
                .isEqualTo(
                    listOf(
                        TupleType(
                            DynamicArrayType(StringType),
                            DynamicArrayType(UintType)
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyDeserializeTupleArray() {
        val json =
            """{
              |  "args": [
              |    {
              |      "type": "tuple[]",
              |      "elems": ["string", "uint"]
              |    }
              |  ]
              |}
            """.trimMargin()

        verify("must correctly parse tuple array") {
            val result = objectMapper.readValue(json, Result::class.java).args.map { it.deserializedType }

            expectThat(result)
                .isEqualTo(
                    listOf(
                        DynamicArrayType(
                            TupleType(StringType, UintType)
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyDeserializeArrayOfNestedTuplesWithArrays() {
        val json =
            """{
              |  "args": [
              |    {
              |      "type": "tuple[][2]",
              |      "elems": [
              |        "string[][1]",
              |        {
              |          "type": "tuple[]",
              |          "elems": [
              |            "address[]",
              |            {
              |              "type": "tuple",
              |              "elems": ["int"]
              |            },
              |            "bool",
              |            {
              |              "type": "tuple[][]",
              |              "elems": ["uint"]
              |            }
              |          ]
              |        },
              |        "uint"
              |      ]
              |    }
              |  ]
              |}
            """.trimMargin()

        verify("must correctly parse array of nested tuples with arrays") {
            val result = objectMapper.readValue(json, Result::class.java).args.map { it.deserializedType }

            expectThat(result)
                .isEqualTo(
                    listOf(
                        StaticArrayType(
                            DynamicArrayType(
                                TupleType(
                                    StaticArrayType(DynamicArrayType(StringType), 1),
                                    DynamicArrayType(
                                        TupleType(
                                            DynamicArrayType(AddressType),
                                            TupleType(IntType),
                                            BoolType,
                                            DynamicArrayType(DynamicArrayType(TupleType(UintType)))
                                        )
                                    ),
                                    UintType
                                )
                            ),
                            2
                        )
                    )
                )
        }
    }

    @Test
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseForUnknownType() {
        val json =
            """{
              |  "args": ["unknown-type"]
              |}
            """.trimMargin()

        verify("JsonMappingException is thrown") {
            val ex = expectThrows<JsonMappingException> {
                objectMapper.readValue(json, Result::class.java)
            }

            expectThat(ex.cause)
                .isInstanceOf(JsonParseException::class.java)
        }
    }

    @Test
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseForInvalidValueType() {
        val json =
            """{
              |  "args": [[]]
              |}
            """.trimMargin()

        verify("JsonMappingException is thrown") {
            val ex = expectThrows<JsonMappingException> {
                objectMapper.readValue(json, Result::class.java)
            }

            expectThat(ex.cause)
                .isInstanceOf(JsonParseException::class.java)
        }
    }

    @Test
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseForMissingTupleType() {
        val json =
            """{
              |  "args": [
              |    {
              |      "elems": []
              |    }
              |  ]
              |}
            """.trimMargin()

        verify("JsonMappingException is thrown") {
            val ex = expectThrows<JsonMappingException> {
                objectMapper.readValue(json, Result::class.java)
            }

            expectThat(ex.cause)
                .isInstanceOf(JsonParseException::class.java)
        }
    }

    @Test
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseForInvalidTupleType() {
        val json =
            """{
              |  "args": [
              |    {
              |      "type": "non-tuple",
              |      "elems": []
              |    }
              |  ]
              |}
            """.trimMargin()

        verify("JsonMappingException is thrown") {
            val ex = expectThrows<JsonMappingException> {
                objectMapper.readValue(json, Result::class.java)
            }

            expectThat(ex.cause)
                .isInstanceOf(JsonParseException::class.java)
        }
    }

    @Test
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseForMissingTupleElements() {
        val json =
            """{
              |  "args": [
              |    {
              |      "type": "tuple"
              |    }
              |  ]
              |}
            """.trimMargin()

        verify("JsonMappingException is thrown") {
            val ex = expectThrows<JsonMappingException> {
                objectMapper.readValue(json, Result::class.java)
            }

            expectThat(ex.cause)
                .isInstanceOf(JsonParseException::class.java)
        }
    }

    @Test
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseForInvalidTupleElement() {
        val json =
            """{
              |  "args": [
              |    {
              |      "type": "tuple",
              |      "elems": [[]]
              |    }
              |  ]
              |}
            """.trimMargin()

        verify("JsonMappingException is thrown") {
            val ex = expectThrows<JsonMappingException> {
                objectMapper.readValue(json, Result::class.java)
            }

            expectThat(ex.cause)
                .isInstanceOf(JsonParseException::class.java)
        }
    }
}
