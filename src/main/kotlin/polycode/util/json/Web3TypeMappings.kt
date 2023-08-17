package polycode.util.json

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JsonNode
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Bool
import org.web3j.abi.datatypes.DynamicBytes
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.Uint
import org.web3j.abi.datatypes.Utf8String
import org.web3j.abi.datatypes.generated.Bytes1
import org.web3j.abi.datatypes.generated.Bytes10
import org.web3j.abi.datatypes.generated.Bytes11
import org.web3j.abi.datatypes.generated.Bytes12
import org.web3j.abi.datatypes.generated.Bytes13
import org.web3j.abi.datatypes.generated.Bytes14
import org.web3j.abi.datatypes.generated.Bytes15
import org.web3j.abi.datatypes.generated.Bytes16
import org.web3j.abi.datatypes.generated.Bytes17
import org.web3j.abi.datatypes.generated.Bytes18
import org.web3j.abi.datatypes.generated.Bytes19
import org.web3j.abi.datatypes.generated.Bytes2
import org.web3j.abi.datatypes.generated.Bytes20
import org.web3j.abi.datatypes.generated.Bytes21
import org.web3j.abi.datatypes.generated.Bytes22
import org.web3j.abi.datatypes.generated.Bytes23
import org.web3j.abi.datatypes.generated.Bytes24
import org.web3j.abi.datatypes.generated.Bytes25
import org.web3j.abi.datatypes.generated.Bytes26
import org.web3j.abi.datatypes.generated.Bytes27
import org.web3j.abi.datatypes.generated.Bytes28
import org.web3j.abi.datatypes.generated.Bytes29
import org.web3j.abi.datatypes.generated.Bytes3
import org.web3j.abi.datatypes.generated.Bytes30
import org.web3j.abi.datatypes.generated.Bytes31
import org.web3j.abi.datatypes.generated.Bytes32
import org.web3j.abi.datatypes.generated.Bytes4
import org.web3j.abi.datatypes.generated.Bytes5
import org.web3j.abi.datatypes.generated.Bytes6
import org.web3j.abi.datatypes.generated.Bytes7
import org.web3j.abi.datatypes.generated.Bytes8
import org.web3j.abi.datatypes.generated.Bytes9
import org.web3j.abi.datatypes.generated.Int104
import org.web3j.abi.datatypes.generated.Int112
import org.web3j.abi.datatypes.generated.Int120
import org.web3j.abi.datatypes.generated.Int128
import org.web3j.abi.datatypes.generated.Int136
import org.web3j.abi.datatypes.generated.Int144
import org.web3j.abi.datatypes.generated.Int152
import org.web3j.abi.datatypes.generated.Int16
import org.web3j.abi.datatypes.generated.Int160
import org.web3j.abi.datatypes.generated.Int168
import org.web3j.abi.datatypes.generated.Int176
import org.web3j.abi.datatypes.generated.Int184
import org.web3j.abi.datatypes.generated.Int192
import org.web3j.abi.datatypes.generated.Int200
import org.web3j.abi.datatypes.generated.Int208
import org.web3j.abi.datatypes.generated.Int216
import org.web3j.abi.datatypes.generated.Int224
import org.web3j.abi.datatypes.generated.Int232
import org.web3j.abi.datatypes.generated.Int24
import org.web3j.abi.datatypes.generated.Int240
import org.web3j.abi.datatypes.generated.Int248
import org.web3j.abi.datatypes.generated.Int256
import org.web3j.abi.datatypes.generated.Int32
import org.web3j.abi.datatypes.generated.Int40
import org.web3j.abi.datatypes.generated.Int48
import org.web3j.abi.datatypes.generated.Int56
import org.web3j.abi.datatypes.generated.Int64
import org.web3j.abi.datatypes.generated.Int72
import org.web3j.abi.datatypes.generated.Int8
import org.web3j.abi.datatypes.generated.Int80
import org.web3j.abi.datatypes.generated.Int88
import org.web3j.abi.datatypes.generated.Int96
import org.web3j.abi.datatypes.generated.Uint104
import org.web3j.abi.datatypes.generated.Uint112
import org.web3j.abi.datatypes.generated.Uint120
import org.web3j.abi.datatypes.generated.Uint128
import org.web3j.abi.datatypes.generated.Uint136
import org.web3j.abi.datatypes.generated.Uint144
import org.web3j.abi.datatypes.generated.Uint152
import org.web3j.abi.datatypes.generated.Uint16
import org.web3j.abi.datatypes.generated.Uint160
import org.web3j.abi.datatypes.generated.Uint168
import org.web3j.abi.datatypes.generated.Uint176
import org.web3j.abi.datatypes.generated.Uint184
import org.web3j.abi.datatypes.generated.Uint192
import org.web3j.abi.datatypes.generated.Uint200
import org.web3j.abi.datatypes.generated.Uint208
import org.web3j.abi.datatypes.generated.Uint216
import org.web3j.abi.datatypes.generated.Uint224
import org.web3j.abi.datatypes.generated.Uint232
import org.web3j.abi.datatypes.generated.Uint24
import org.web3j.abi.datatypes.generated.Uint240
import org.web3j.abi.datatypes.generated.Uint248
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.abi.datatypes.generated.Uint32
import org.web3j.abi.datatypes.generated.Uint40
import org.web3j.abi.datatypes.generated.Uint48
import org.web3j.abi.datatypes.generated.Uint56
import org.web3j.abi.datatypes.generated.Uint64
import org.web3j.abi.datatypes.generated.Uint72
import org.web3j.abi.datatypes.generated.Uint8
import org.web3j.abi.datatypes.generated.Uint80
import org.web3j.abi.datatypes.generated.Uint88
import org.web3j.abi.datatypes.generated.Uint96
import polycode.features.contract.abi.model.AbiType
import polycode.features.contract.abi.model.AddressType
import polycode.features.contract.abi.model.BoolType
import polycode.features.contract.abi.model.DynamicBytesType
import polycode.features.contract.abi.model.IntType
import polycode.features.contract.abi.model.StaticBytesType
import polycode.features.contract.abi.model.StringType
import polycode.features.contract.abi.model.UintType
import java.math.BigInteger
import kotlin.Int
import org.web3j.abi.datatypes.Int as Web3Int
import org.web3j.abi.datatypes.primitive.Byte as Web3Byte

object Web3TypeMappings {

    private data class TypeInfo(
        val abiType: AbiType,
        val parseFn: (JsonNode, JsonParser, Boolean) -> Type<*>
    )

    private const val VALUE_ERROR = "invalid value type"
    private val SIMPLE_TYPE_MAPPINGS: Map<String, TypeInfo> = mapOf(
        "address" to TypeInfo(AddressType) { v: JsonNode, p: JsonParser, t: Boolean -> Address(v.parseText(p, t)) },
        "bool" to TypeInfo(BoolType) { v: JsonNode, p: JsonParser, t: Boolean -> Bool(v.parseBoolean(p, t)) },
        "string" to TypeInfo(StringType) { v: JsonNode, p: JsonParser, t: Boolean -> Utf8String(v.parseText(p, t)) },
        "bytes" to TypeInfo(DynamicBytesType) { v: JsonNode, p: JsonParser, t: Boolean ->
            DynamicBytes(v.parseBytes(p, null, t))
        },
        "byte" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean ->
            Web3Byte(v.parseBigInt(p, t).toByte())
        }
    )
    private val UINT_TYPE_MAPPINGS: Map<String, TypeInfo> = mapOf(
        "uint" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint(v.parseBigInt(p, t)) },
        "uint8" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint8(v.parseBigInt(p, t)) },
        "uint16" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint16(v.parseBigInt(p, t)) },
        "uint24" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint24(v.parseBigInt(p, t)) },
        "uint32" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint32(v.parseBigInt(p, t)) },
        "uint40" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint40(v.parseBigInt(p, t)) },
        "uint48" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint48(v.parseBigInt(p, t)) },
        "uint56" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint56(v.parseBigInt(p, t)) },
        "uint64" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint64(v.parseBigInt(p, t)) },
        "uint72" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint72(v.parseBigInt(p, t)) },
        "uint80" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint80(v.parseBigInt(p, t)) },
        "uint88" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint88(v.parseBigInt(p, t)) },
        "uint96" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint96(v.parseBigInt(p, t)) },
        "uint104" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint104(v.parseBigInt(p, t)) },
        "uint112" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint112(v.parseBigInt(p, t)) },
        "uint120" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint120(v.parseBigInt(p, t)) },
        "uint128" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint128(v.parseBigInt(p, t)) },
        "uint136" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint136(v.parseBigInt(p, t)) },
        "uint144" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint144(v.parseBigInt(p, t)) },
        "uint152" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint152(v.parseBigInt(p, t)) },
        "uint160" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint160(v.parseBigInt(p, t)) },
        "uint168" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint168(v.parseBigInt(p, t)) },
        "uint176" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint176(v.parseBigInt(p, t)) },
        "uint184" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint184(v.parseBigInt(p, t)) },
        "uint192" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint192(v.parseBigInt(p, t)) },
        "uint200" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint200(v.parseBigInt(p, t)) },
        "uint208" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint208(v.parseBigInt(p, t)) },
        "uint216" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint216(v.parseBigInt(p, t)) },
        "uint224" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint224(v.parseBigInt(p, t)) },
        "uint232" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint232(v.parseBigInt(p, t)) },
        "uint240" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint240(v.parseBigInt(p, t)) },
        "uint248" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint248(v.parseBigInt(p, t)) },
        "uint256" to TypeInfo(UintType) { v: JsonNode, p: JsonParser, t: Boolean -> Uint256(v.parseBigInt(p, t)) }
    )
    private val INT_TYPE_MAPPINGS: Map<String, TypeInfo> = mapOf(
        "int" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Web3Int(v.parseBigInt(p, t)) },
        "int8" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int8(v.parseBigInt(p, t)) },
        "int16" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int16(v.parseBigInt(p, t)) },
        "int24" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int24(v.parseBigInt(p, t)) },
        "int32" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int32(v.parseBigInt(p, t)) },
        "int40" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int40(v.parseBigInt(p, t)) },
        "int48" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int48(v.parseBigInt(p, t)) },
        "int56" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int56(v.parseBigInt(p, t)) },
        "int64" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int64(v.parseBigInt(p, t)) },
        "int72" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int72(v.parseBigInt(p, t)) },
        "int80" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int80(v.parseBigInt(p, t)) },
        "int88" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int88(v.parseBigInt(p, t)) },
        "int96" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int96(v.parseBigInt(p, t)) },
        "int104" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int104(v.parseBigInt(p, t)) },
        "int112" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int112(v.parseBigInt(p, t)) },
        "int120" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int120(v.parseBigInt(p, t)) },
        "int128" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int128(v.parseBigInt(p, t)) },
        "int136" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int136(v.parseBigInt(p, t)) },
        "int144" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int144(v.parseBigInt(p, t)) },
        "int152" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int152(v.parseBigInt(p, t)) },
        "int160" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int160(v.parseBigInt(p, t)) },
        "int168" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int168(v.parseBigInt(p, t)) },
        "int176" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int176(v.parseBigInt(p, t)) },
        "int184" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int184(v.parseBigInt(p, t)) },
        "int192" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int192(v.parseBigInt(p, t)) },
        "int200" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int200(v.parseBigInt(p, t)) },
        "int208" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int208(v.parseBigInt(p, t)) },
        "int216" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int216(v.parseBigInt(p, t)) },
        "int224" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int224(v.parseBigInt(p, t)) },
        "int232" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int232(v.parseBigInt(p, t)) },
        "int240" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int240(v.parseBigInt(p, t)) },
        "int248" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int248(v.parseBigInt(p, t)) },
        "int256" to TypeInfo(IntType) { v: JsonNode, p: JsonParser, t: Boolean -> Int256(v.parseBigInt(p, t)) }
    )

    @Suppress("MagicNumber")
    private val BYTES_TYPE_MAPPINGS: Map<String, TypeInfo> = mapOf(
        "bytes1" to TypeInfo(StaticBytesType(1)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes1(v.parseBytes(p, length = 1, t))
        },
        "bytes2" to TypeInfo(StaticBytesType(2)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes2(v.parseBytes(p, length = 2, t))
        },
        "bytes3" to TypeInfo(StaticBytesType(3)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes3(v.parseBytes(p, length = 3, t))
        },
        "bytes4" to TypeInfo(StaticBytesType(4)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes4(v.parseBytes(p, length = 4, t))
        },
        "bytes5" to TypeInfo(StaticBytesType(5)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes5(v.parseBytes(p, length = 5, t))
        },
        "bytes6" to TypeInfo(StaticBytesType(6)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes6(v.parseBytes(p, length = 6, t))
        },
        "bytes7" to TypeInfo(StaticBytesType(7)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes7(v.parseBytes(p, length = 7, t))
        },
        "bytes8" to TypeInfo(StaticBytesType(8)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes8(v.parseBytes(p, length = 8, t))
        },
        "bytes9" to TypeInfo(StaticBytesType(9)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes9(v.parseBytes(p, length = 9, t))
        },
        "bytes10" to TypeInfo(StaticBytesType(10)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes10(v.parseBytes(p, length = 10, t))
        },
        "bytes11" to TypeInfo(StaticBytesType(11)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes11(v.parseBytes(p, length = 11, t))
        },
        "bytes12" to TypeInfo(StaticBytesType(12)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes12(v.parseBytes(p, length = 12, t))
        },
        "bytes13" to TypeInfo(StaticBytesType(13)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes13(v.parseBytes(p, length = 13, t))
        },
        "bytes14" to TypeInfo(StaticBytesType(14)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes14(v.parseBytes(p, length = 14, t))
        },
        "bytes15" to TypeInfo(StaticBytesType(15)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes15(v.parseBytes(p, length = 15, t))
        },
        "bytes16" to TypeInfo(StaticBytesType(16)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes16(v.parseBytes(p, length = 16, t))
        },
        "bytes17" to TypeInfo(StaticBytesType(17)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes17(v.parseBytes(p, length = 17, t))
        },
        "bytes18" to TypeInfo(StaticBytesType(18)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes18(v.parseBytes(p, length = 18, t))
        },
        "bytes19" to TypeInfo(StaticBytesType(19)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes19(v.parseBytes(p, length = 19, t))
        },
        "bytes20" to TypeInfo(StaticBytesType(20)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes20(v.parseBytes(p, length = 20, t))
        },
        "bytes21" to TypeInfo(StaticBytesType(21)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes21(v.parseBytes(p, length = 21, t))
        },
        "bytes22" to TypeInfo(StaticBytesType(22)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes22(v.parseBytes(p, length = 22, t))
        },
        "bytes23" to TypeInfo(StaticBytesType(23)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes23(v.parseBytes(p, length = 23, t))
        },
        "bytes24" to TypeInfo(StaticBytesType(24)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes24(v.parseBytes(p, length = 24, t))
        },
        "bytes25" to TypeInfo(StaticBytesType(25)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes25(v.parseBytes(p, length = 25, t))
        },
        "bytes26" to TypeInfo(StaticBytesType(26)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes26(v.parseBytes(p, length = 26, t))
        },
        "bytes27" to TypeInfo(StaticBytesType(27)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes27(v.parseBytes(p, length = 27, t))
        },
        "bytes28" to TypeInfo(StaticBytesType(28)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes28(v.parseBytes(p, length = 28, t))
        },
        "bytes29" to TypeInfo(StaticBytesType(29)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes29(v.parseBytes(p, length = 29, t))
        },
        "bytes30" to TypeInfo(StaticBytesType(30)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes30(v.parseBytes(p, length = 30, t))
        },
        "bytes31" to TypeInfo(StaticBytesType(31)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes31(v.parseBytes(p, length = 31, t))
        },
        "bytes32" to TypeInfo(StaticBytesType(32)) { v: JsonNode, p: JsonParser, t: Boolean ->
            Bytes32(v.parseBytes(p, length = 32, t))
        }
    )
    private val TYPE_MAPPINGS: Map<String, TypeInfo> = SIMPLE_TYPE_MAPPINGS + UINT_TYPE_MAPPINGS +
        INT_TYPE_MAPPINGS + BYTES_TYPE_MAPPINGS

    operator fun get(argumentType: String): ((JsonNode, JsonParser) -> Type<*>)? =
        TYPE_MAPPINGS[argumentType]?.let { { v: JsonNode, p: JsonParser -> it.parseFn.invoke(v, p, false) } }

    fun getWeb3Type(p: JsonParser, value: JsonNode, argumentType: String): Class<Type<*>>? =
        TYPE_MAPPINGS[argumentType]?.parseFn?.invoke(value, p, true)?.javaClass

    fun getAbiType(argumentType: String): AbiType? = TYPE_MAPPINGS[argumentType]?.abiType

    private fun JsonNode.parseText(p: JsonParser, getDefault: Boolean): String =
        if (getDefault) "0x0" else if (this.isTextual) this.asText() else throw JsonParseException(p, VALUE_ERROR)

    private fun JsonNode.parseBoolean(p: JsonParser, getDefault: Boolean): Boolean =
        if (getDefault) false else if (this.isBoolean) this.asBoolean() else throw JsonParseException(p, VALUE_ERROR)

    private fun JsonNode.parseBigInt(p: JsonParser, getDefault: Boolean): BigInteger =
        if (getDefault) {
            BigInteger.ZERO
        } else if (this.isNumber) {
            this.bigIntegerValue()
        } else if (this.isTextual) {
            BigInteger(this.asText())
        } else {
            throw JsonParseException(p, VALUE_ERROR)
        }

    private fun JsonNode.parseBytes(p: JsonParser, length: Int?, getDefault: Boolean): ByteArray =
        if (getDefault) {
            ByteArray(length ?: 0)
        } else if (this.isArray) {
            this.elements().asSequence().map { it.parseBigInt(p, false).toByte() }.toList().toByteArray().takeIf {
                length == null || it.size == length
            } ?: throw JsonParseException(p, "invalid byte array length")
        } else {
            throw JsonParseException(p, VALUE_ERROR)
        }
}
