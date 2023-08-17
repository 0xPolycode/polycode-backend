package polycode.util.json

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import org.junit.jupiter.api.Test
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Bool
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.DynamicBytes
import org.web3j.abi.datatypes.DynamicStruct
import org.web3j.abi.datatypes.StaticStruct
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
import polycode.TestBase
import polycode.config.JsonConfig
import polycode.features.functions.encoding.model.FunctionArgument
import polycode.util.SizedStaticArray
import java.math.BigInteger
import org.web3j.abi.datatypes.Int as Web3Int
import org.web3j.abi.datatypes.primitive.Byte as Web3Byte

class FunctionArgumentJsonDeserializerTest : TestBase() {

    companion object {
        private data class Result(val args: List<FunctionArgument>)
    }

    private val objectMapper = JsonConfig().objectMapper()

    @Test
    fun mustCorrectlyDeserializeBaseTypes() {
        val address = Address("cafebabe")
        val bool = Bool(true)
        val string = Utf8String("test-string")
        val bytes = DynamicBytes(byteArrayOf(1, 2, 3))
        val byte = Web3Byte(1)
        val uint = Uint(BigInteger.ONE)
        val uint8 = Uint8(8L)
        val uint16 = Uint16(16L)
        val uint24 = Uint24(24L)
        val uint32 = Uint32(32L)
        val uint40 = Uint40(40L)
        val uint48 = Uint48(48L)
        val uint56 = Uint56(56L)
        val uint64 = Uint64(64L)
        val uint72 = Uint72(72L)
        val uint80 = Uint80(80L)
        val uint88 = Uint88(88L)
        val uint96 = Uint96(96L)
        val uint104 = Uint104(104L)
        val uint112 = Uint112(112L)
        val uint120 = Uint120(120L)
        val uint128 = Uint128(128L)
        val uint136 = Uint136(136L)
        val uint144 = Uint144(144L)
        val uint152 = Uint152(152L)
        val uint160 = Uint160(160L)
        val uint168 = Uint168(168L)
        val uint176 = Uint176(176L)
        val uint184 = Uint184(184L)
        val uint192 = Uint192(192L)
        val uint200 = Uint200(200L)
        val uint208 = Uint208(208L)
        val uint216 = Uint216(216L)
        val uint224 = Uint224(224L)
        val uint232 = Uint232(232L)
        val uint240 = Uint240(240L)
        val uint248 = Uint248(248L)
        val uint256 = Uint256(256L)
        val int = Web3Int(BigInteger.ONE)
        val int8 = Int8(8L)
        val int16 = Int16(16L)
        val int24 = Int24(24L)
        val int32 = Int32(32L)
        val int40 = Int40(40L)
        val int48 = Int48(48L)
        val int56 = Int56(56L)
        val int64 = Int64(64L)
        val int72 = Int72(72L)
        val int80 = Int80(80L)
        val int88 = Int88(88L)
        val int96 = Int96(96L)
        val int104 = Int104(104L)
        val int112 = Int112(112L)
        val int120 = Int120(120L)
        val int128 = Int128(128L)
        val int136 = Int136(136L)
        val int144 = Int144(144L)
        val int152 = Int152(152L)
        val int160 = Int160(160L)
        val int168 = Int168(168L)
        val int176 = Int176(176L)
        val int184 = Int184(184L)
        val int192 = Int192(192L)
        val int200 = Int200(200L)
        val int208 = Int208(208L)
        val int216 = Int216(216L)
        val int224 = Int224(224L)
        val int232 = Int232(232L)
        val int240 = Int240(240L)
        val int248 = Int248(248L)
        val int256 = Int256(256L)
        val bytes1 = Bytes1(ByteArray(1) { it.toByte() })
        val bytes2 = Bytes2(ByteArray(2) { it.toByte() })
        val bytes3 = Bytes3(ByteArray(3) { it.toByte() })
        val bytes4 = Bytes4(ByteArray(4) { it.toByte() })
        val bytes5 = Bytes5(ByteArray(5) { it.toByte() })
        val bytes6 = Bytes6(ByteArray(6) { it.toByte() })
        val bytes7 = Bytes7(ByteArray(7) { it.toByte() })
        val bytes8 = Bytes8(ByteArray(8) { it.toByte() })
        val bytes9 = Bytes9(ByteArray(9) { it.toByte() })
        val bytes10 = Bytes10(ByteArray(10) { it.toByte() })
        val bytes11 = Bytes11(ByteArray(11) { it.toByte() })
        val bytes12 = Bytes12(ByteArray(12) { it.toByte() })
        val bytes13 = Bytes13(ByteArray(13) { it.toByte() })
        val bytes14 = Bytes14(ByteArray(14) { it.toByte() })
        val bytes15 = Bytes15(ByteArray(15) { it.toByte() })
        val bytes16 = Bytes16(ByteArray(16) { it.toByte() })
        val bytes17 = Bytes17(ByteArray(17) { it.toByte() })
        val bytes18 = Bytes18(ByteArray(18) { it.toByte() })
        val bytes19 = Bytes19(ByteArray(19) { it.toByte() })
        val bytes20 = Bytes20(ByteArray(20) { it.toByte() })
        val bytes21 = Bytes21(ByteArray(21) { it.toByte() })
        val bytes22 = Bytes22(ByteArray(22) { it.toByte() })
        val bytes23 = Bytes23(ByteArray(23) { it.toByte() })
        val bytes24 = Bytes24(ByteArray(24) { it.toByte() })
        val bytes25 = Bytes25(ByteArray(25) { it.toByte() })
        val bytes26 = Bytes26(ByteArray(26) { it.toByte() })
        val bytes27 = Bytes27(ByteArray(27) { it.toByte() })
        val bytes28 = Bytes28(ByteArray(28) { it.toByte() })
        val bytes29 = Bytes29(ByteArray(29) { it.toByte() })
        val bytes30 = Bytes30(ByteArray(30) { it.toByte() })
        val bytes31 = Bytes31(ByteArray(31) { it.toByte() })
        val bytes32 = Bytes32(ByteArray(32) { it.toByte() })
        val allValues = listOf(
            address, bool, string, bytes, byte, uint, uint8, uint16, uint24, uint32, uint40, uint48, uint56, uint64,
            uint72, uint80, uint88, uint96, uint104, uint112, uint120, uint128, uint136, uint144, uint152, uint160,
            uint168, uint176, uint184, uint192, uint200, uint208, uint216, uint224, uint232, uint240, uint248, uint256,
            int, int8, int16, int24, int32, int40, int48, int56, int64, int72, int80, int88, int96, int104, int112,
            int120, int128, int136, int144, int152, int160, int168, int176, int184, int192, int200, int208, int216,
            int224, int232, int240, int248, int256, bytes1, bytes2, bytes3, bytes4, bytes5, bytes6, bytes7, bytes8,
            bytes9, bytes10, bytes11, bytes12, bytes13, bytes14, bytes15, bytes16, bytes17, bytes18, bytes19, bytes20,
            bytes21, bytes22, bytes23, bytes24, bytes25, bytes26, bytes27, bytes28, bytes29, bytes30, bytes31, bytes32
        )

        val json =
            """{
              |  "args": [
              |    {
              |      "type": "address",
              |      "value": "${address.value}"
              |    },
              |    {
              |      "type": "bool",
              |      "value": ${bool.value}
              |    },
              |    {
              |      "type": "string",
              |      "value": "${string.value}"
              |    },
              |    {
              |      "type": "bytes",
              |      "value": ${bytes.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "byte",
              |      "value": ${byte.value}
              |    },
              |    {
              |      "type": "uint",
              |      "value": "${uint.value}"
              |    },
              |    {
              |      "type": "uint8",
              |      "value": ${uint8.value}
              |    },
              |    {
              |      "type": "uint16",
              |      "value": ${uint16.value}
              |    },
              |    {
              |      "type": "uint24",
              |      "value": ${uint24.value}
              |    },
              |    {
              |      "type": "uint32",
              |      "value": ${uint32.value}
              |    },
              |    {
              |      "type": "uint40",
              |      "value": ${uint40.value}
              |    },
              |    {
              |      "type": "uint48",
              |      "value": ${uint48.value}
              |    },
              |    {
              |      "type": "uint56",
              |      "value": ${uint56.value}
              |    },
              |    {
              |      "type": "uint64",
              |      "value": ${uint64.value}
              |    },
              |    {
              |      "type": "uint72",
              |      "value": ${uint72.value}
              |    },
              |    {
              |      "type": "uint80",
              |      "value": ${uint80.value}
              |    },
              |    {
              |      "type": "uint88",
              |      "value": ${uint88.value}
              |    },
              |    {
              |      "type": "uint96",
              |      "value": ${uint96.value}
              |    },
              |    {
              |      "type": "uint104",
              |      "value": ${uint104.value}
              |    },
              |    {
              |      "type": "uint112",
              |      "value": ${uint112.value}
              |    },
              |    {
              |      "type": "uint120",
              |      "value": ${uint120.value}
              |    },
              |    {
              |      "type": "uint128",
              |      "value": ${uint128.value}
              |    },
              |    {
              |      "type": "uint136",
              |      "value": ${uint136.value}
              |    },
              |    {
              |      "type": "uint144",
              |      "value": ${uint144.value}
              |    },
              |    {
              |      "type": "uint152",
              |      "value": ${uint152.value}
              |    },
              |    {
              |      "type": "uint160",
              |      "value": ${uint160.value}
              |    },
              |    {
              |      "type": "uint168",
              |      "value": ${uint168.value}
              |    },
              |    {
              |      "type": "uint176",
              |      "value": ${uint176.value}
              |    },
              |    {
              |      "type": "uint184",
              |      "value": ${uint184.value}
              |    },
              |    {
              |      "type": "uint192",
              |      "value": ${uint192.value}
              |    },
              |    {
              |      "type": "uint200",
              |      "value": ${uint200.value}
              |    },
              |    {
              |      "type": "uint208",
              |      "value": ${uint208.value}
              |    },
              |    {
              |      "type": "uint216",
              |      "value": ${uint216.value}
              |    },
              |    {
              |      "type": "uint224",
              |      "value": ${uint224.value}
              |    },
              |    {
              |      "type": "uint232",
              |      "value": ${uint232.value}
              |    },
              |    {
              |      "type": "uint240",
              |      "value": ${uint240.value}
              |    },
              |    {
              |      "type": "uint248",
              |      "value": ${uint248.value}
              |    },
              |    {
              |      "type": "uint256",
              |      "value": ${uint256.value}
              |    },
              |    {
              |      "type": "int",
              |      "value": "${int.value}"
              |    },
              |    {
              |      "type": "int8",
              |      "value": ${int8.value}
              |    },
              |    {
              |      "type": "int16",
              |      "value": ${int16.value}
              |    },
              |    {
              |      "type": "int24",
              |      "value": ${int24.value}
              |    },
              |    {
              |      "type": "int32",
              |      "value": ${int32.value}
              |    },
              |    {
              |      "type": "int40",
              |      "value": ${int40.value}
              |    },
              |    {
              |      "type": "int48",
              |      "value": ${int48.value}
              |    },
              |    {
              |      "type": "int56",
              |      "value": ${int56.value}
              |    },
              |    {
              |      "type": "int64",
              |      "value": ${int64.value}
              |    },
              |    {
              |      "type": "int72",
              |      "value": ${int72.value}
              |    },
              |    {
              |      "type": "int80",
              |      "value": ${int80.value}
              |    },
              |    {
              |      "type": "int88",
              |      "value": ${int88.value}
              |    },
              |    {
              |      "type": "int96",
              |      "value": ${int96.value}
              |    },
              |    {
              |      "type": "int104",
              |      "value": ${int104.value}
              |    },
              |    {
              |      "type": "int112",
              |      "value": ${int112.value}
              |    },
              |    {
              |      "type": "int120",
              |      "value": ${int120.value}
              |    },
              |    {
              |      "type": "int128",
              |      "value": ${int128.value}
              |    },
              |    {
              |      "type": "int136",
              |      "value": ${int136.value}
              |    },
              |    {
              |      "type": "int144",
              |      "value": ${int144.value}
              |    },
              |    {
              |      "type": "int152",
              |      "value": ${int152.value}
              |    },
              |    {
              |      "type": "int160",
              |      "value": ${int160.value}
              |    },
              |    {
              |      "type": "int168",
              |      "value": ${int168.value}
              |    },
              |    {
              |      "type": "int176",
              |      "value": ${int176.value}
              |    },
              |    {
              |      "type": "int184",
              |      "value": ${int184.value}
              |    },
              |    {
              |      "type": "int192",
              |      "value": ${int192.value}
              |    },
              |    {
              |      "type": "int200",
              |      "value": ${int200.value}
              |    },
              |    {
              |      "type": "int208",
              |      "value": ${int208.value}
              |    },
              |    {
              |      "type": "int216",
              |      "value": ${int216.value}
              |    },
              |    {
              |      "type": "int224",
              |      "value": ${int224.value}
              |    },
              |    {
              |      "type": "int232",
              |      "value": ${int232.value}
              |    },
              |    {
              |      "type": "int240",
              |      "value": ${int240.value}
              |    },
              |    {
              |      "type": "int248",
              |      "value": ${int248.value}
              |    },
              |    {
              |      "type": "int256",
              |      "value": ${int256.value}
              |    },
              |    {
              |      "type": "bytes1",
              |      "value": ${bytes1.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes2",
              |      "value": ${bytes2.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes3",
              |      "value": ${bytes3.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes4",
              |      "value": ${bytes4.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes5",
              |      "value": ${bytes5.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes6",
              |      "value": ${bytes6.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes7",
              |      "value": ${bytes7.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes8",
              |      "value": ${bytes8.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes9",
              |      "value": ${bytes9.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes10",
              |      "value": ${bytes10.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes11",
              |      "value": ${bytes11.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes12",
              |      "value": ${bytes12.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes13",
              |      "value": ${bytes13.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes14",
              |      "value": ${bytes14.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes15",
              |      "value": ${bytes15.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes16",
              |      "value": ${bytes16.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes17",
              |      "value": ${bytes17.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes18",
              |      "value": ${bytes18.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes19",
              |      "value": ${bytes19.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes20",
              |      "value": ${bytes20.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes21",
              |      "value": ${bytes21.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes22",
              |      "value": ${bytes22.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes23",
              |      "value": ${bytes23.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes24",
              |      "value": ${bytes24.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes25",
              |      "value": ${bytes25.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes26",
              |      "value": ${bytes26.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes27",
              |      "value": ${bytes27.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes28",
              |      "value": ${bytes28.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes29",
              |      "value": ${bytes29.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes30",
              |      "value": ${bytes30.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes31",
              |      "value": ${bytes31.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    },
              |    {
              |      "type": "bytes32",
              |      "value": ${bytes32.value.joinToString(prefix = "[", separator = ", ", postfix = "]")}
              |    }
              |  ]
              |}
            """.trimMargin()

        verify("must correctly parse base types") {
            val result = objectMapper.readValue(json, Result::class.java).args
                .map { it.value.value }
                .map {
                    // array equality in Java does not work, so we need to compare lists
                    if (it.javaClass == ByteArray::class.java) {
                        (it as ByteArray).asList()
                    } else {
                        it
                    }
                }

            expectThat(result)
                .isEqualTo(
                    allValues.map { it.value }
                        .map {
                            if (it.javaClass == ByteArray::class.java) {
                                (it as ByteArray).asList()
                            } else {
                                it
                            }
                        }
                )
        }
    }

    @Test
    fun mustCorrectlyDeserializeEmptyArrayType() {
        val json =
            """{
              |  "args": [
              |    {
              |      "type": "bytes[]",
              |      "value": []
              |    }
              |  ]
              |}
            """.trimMargin()

        verify("must correctly parse empty dynamic array type") {
            val (componentType, result) = objectMapper.readValue(json, Result::class.java).args
                .map {
                    @Suppress("UNCHECKED_CAST")
                    it.value as DynamicArray<DynamicBytes>
                }
                .map { Pair(it.componentType, it.value.map { bytes -> bytes.value }) }[0]

            expectThat(componentType)
                .isEqualTo(DynamicBytes::class.java)
            expectThat(result)
                .isEmpty()
        }
    }

    @Test
    fun mustCorrectlyDeserializeDynamicArrayType() {
        val json =
            """{
              |  "args": [
              |    {
              |      "type": "string[]",
              |      "value": ["a", "b"]
              |    }
              |  ]
              |}
            """.trimMargin()

        verify("must correctly parse dynamic array type") {
            val (componentType, result) = objectMapper.readValue(json, Result::class.java).args
                .map {
                    @Suppress("UNCHECKED_CAST")
                    it.value as DynamicArray<Utf8String>
                }
                .map { Pair(it.componentType, it.value.map { utf8string -> utf8string.value }) }[0]

            expectThat(componentType)
                .isEqualTo(Utf8String::class.java)
            expectThat(result)
                .isEqualTo(listOf("a", "b"))
        }
    }

    @Test
    fun mustCorrectlyDeserializeZeroSizedArrayType() {
        val json =
            """{
              |  "args": [
              |    {
              |      "type": "uint[0]",
              |      "value": []
              |    }
              |  ]
              |}
            """.trimMargin()

        verify("must correctly parse zero-sized array type") {
            val (componentType, result) = objectMapper.readValue(json, Result::class.java).args
                .map {
                    @Suppress("UNCHECKED_CAST")
                    it.value as SizedStaticArray<Uint>
                }
                .map { Pair(it.componentType, it.value.map { uint -> uint.value }) }[0]

            expectThat(componentType)
                .isEqualTo(Uint::class.java)
            expectThat(result)
                .isEmpty()
        }
    }

    @Test
    fun mustCorrectlyDeserializeSizedArrayType() {
        val json =
            """{
              |  "args": [
              |    {
              |      "type": "bool[2]",
              |      "value": [true, false]
              |    }
              |  ]
              |}
            """.trimMargin()

        verify("must correctly parse sized array type") {
            val (componentType, result) = objectMapper.readValue(json, Result::class.java).args
                .map {
                    @Suppress("UNCHECKED_CAST")
                    it.value as SizedStaticArray<Bool>
                }
                .map { Pair(it.componentType, it.value.map { bool -> bool.value }) }[0]

            expectThat(componentType)
                .isEqualTo(Bool::class.java)
            expectThat(result)
                .isEqualTo(listOf(true, false))
        }
    }

    @Test
    fun mustCorrectlyDeserializeNestedArrayType() {
        val json =
            """{
              |  "args": [
              |    {
              |      "type": "uint[][2][3]",
              |      "value": [
              |        [
              |          [], [1]
              |        ],
              |        [
              |          [2, 3],
              |          [4, 5, 6]
              |        ],
              |        [
              |          [7, 8, 9, 10],
              |          [11, 12, 13, 14, 15]
              |        ]
              |      ]
              |    }
              |  ]
              |}
            """.trimMargin()

        verify("must correctly parse nested array type") {
            val readValue = objectMapper.readValue(json, Result::class.java).args
                .map {
                    @Suppress("UNCHECKED_CAST")
                    it.value as SizedStaticArray<SizedStaticArray<DynamicArray<Uint>>>
                }
            val result = readValue.map {
                it.value.map { i ->
                    i.value.map { j ->
                        j.value.map { k ->
                            k.value.intValueExact()
                        }
                    }
                }
            }[0]
            val componentTypes = readValue.flatMap {
                val innerArray1 = it.value[0]
                val innerArray2 = innerArray1.value[0]
                listOf(it.componentType, innerArray1.componentType, innerArray2.componentType)
            }

            expectThat(componentTypes)
                .isEqualTo(listOf(SizedStaticArray::class.java, DynamicArray::class.java, Uint::class.java))
            expectThat(result)
                .isEqualTo(
                    listOf(
                        listOf(
                            emptyList(),
                            listOf(1)
                        ),
                        listOf(
                            listOf(2, 3),
                            listOf(4, 5, 6)
                        ),
                        listOf(
                            listOf(7, 8, 9, 10),
                            listOf(11, 12, 13, 14, 15)
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyDeserializeTuple() {
        val json =
            """{
              |  "args": [
              |    {
              |      "type": "tuple",
              |      "value": [
              |        {
              |          "type": "string",
              |          "value": "test"
              |        },
              |        {
              |          "type": "uint",
              |          "value": 1
              |        }
              |      ]
              |    }
              |  ]
              |}
            """.trimMargin()

        verify("must correctly parse tuple") {
            val result = objectMapper.readValue(json, Result::class.java).args
                .map { it.value as DynamicStruct }
                .map { it.value.map { type -> type.value } }[0]

            expectThat(result)
                .isEqualTo(listOf("test", BigInteger.ONE))
        }
    }

    @Test
    fun mustCorrectlyDeserializeNestedTuple() {
        val json =
            """{
              |  "args": [
              |    {
              |      "type": "tuple",
              |      "value": [
              |        {
              |          "type": "string",
              |          "value": "test"
              |        },
              |        {
              |          "type": "tuple",
              |          "value": [
              |            {
              |              "type": "string",
              |              "value": "nested1"
              |            },
              |            {
              |              "type": "tuple",
              |              "value": [
              |                {
              |                  "type": "string",
              |                  "value": "nested2"
              |                },
              |                {
              |                  "type": "bool",
              |                  "value": true
              |                }
              |              ]
              |            },
              |            {
              |              "type": "uint",
              |              "value": 0
              |            }
              |          ]
              |        },
              |        {
              |          "type": "uint",
              |          "value": 1
              |        }
              |      ]
              |    }
              |  ]
              |}
            """.trimMargin()

        verify("must correctly parse nested tuple") {
            val result = objectMapper.readValue(json, Result::class.java).args
                .map { it.value as DynamicStruct }
                .map {
                    it.value.map { type ->
                        if (type is DynamicStruct) {
                            type.value.map { innerValue ->
                                if (innerValue is DynamicStruct) {
                                    innerValue.value.map { v -> v.value }
                                } else {
                                    innerValue.value
                                }
                            }
                        } else {
                            type.value
                        }
                    }
                }[0]

            expectThat(result)
                .isEqualTo(
                    listOf(
                        "test",
                        listOf(
                            "nested1",
                            listOf("nested2", true),
                            BigInteger.ZERO
                        ),
                        BigInteger.ONE
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
              |      "value": [
              |        {
              |          "type": "string[]",
              |          "value": ["test", "another"]
              |        },
              |        {
              |          "type": "uint[]",
              |          "value": [0, 1, 2]
              |        }
              |      ]
              |    }
              |  ]
              |}
            """.trimMargin()

        verify("must correctly parse tuple with array elements") {
            val result = objectMapper.readValue(json, Result::class.java).args
                .map { it.value as DynamicStruct }
                .map {
                    it.value.map { type ->
                        (type as DynamicArray<*>).value.map { s -> s.value }
                    }
                }[0]

            expectThat(result)
                .isEqualTo(
                    listOf(
                        listOf("test", "another"),
                        listOf(BigInteger.ZERO, BigInteger.ONE, BigInteger.TWO)
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
              |      "value": [
              |        [
              |          {
              |            "type": "string",
              |            "value": "tuple1"
              |          },
              |          {
              |            "type": "uint",
              |            "value": 0
              |          }
              |        ],
              |        [
              |          {
              |            "type": "string",
              |            "value": "tuple2"
              |          },
              |          {
              |            "type": "uint",
              |            "value": 1
              |          }
              |        ]
              |      ]
              |    }
              |  ]
              |}
            """.trimMargin()

        verify("must correctly parse tuple array") {
            val result = objectMapper.readValue(json, Result::class.java).args
                .map {
                    @Suppress("UNCHECKED_CAST")
                    it.value as DynamicArray<DynamicStruct>
                }
                .map {
                    it.value.map { tuple -> tuple.value.map { elem -> elem.value } }
                }[0]

            expectThat(result)
                .isEqualTo(
                    listOf(
                        listOf("tuple1", BigInteger.ZERO),
                        listOf("tuple2", BigInteger.ONE)
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
              |      "type": "tuple[]",
              |      "value": [
              |        [
              |          {
              |            "type": "string[][1]",
              |            "value": [["test1"]]
              |          },
              |          {
              |            "type": "tuple[]",
              |            "value": [
              |              [
              |                {
              |                  "type": "address[]",
              |                  "value": ["0x0"]
              |                },
              |                {
              |                  "type": "tuple",
              |                  "value": [
              |                    {
              |                      "type": "int",
              |                      "value": 2
              |                    }
              |                  ]
              |                },
              |                {
              |                  "type": "bool",
              |                  "value": true
              |                },
              |                {
              |                  "type": "tuple[]",
              |                  "value": []
              |                }
              |              ]
              |            ]
              |          },
              |          {
              |            "type": "uint",
              |            "value": 1
              |          }
              |        ],
              |        [
              |          {
              |            "type": "string[][1]",
              |            "value": [["test2"]]
              |          },
              |          {
              |            "type": "tuple[]",
              |            "value": [
              |              [
              |                {
              |                  "type": "address[]",
              |                  "value": ["0x1"]
              |                },
              |                {
              |                  "type": "tuple",
              |                  "value": [
              |                    {
              |                      "type": "int",
              |                      "value": 0
              |                    }
              |                  ]
              |                },
              |                {
              |                  "type": "bool",
              |                  "value": false
              |                },
              |                {
              |                  "type": "tuple[]",
              |                  "value": []
              |                }
              |              ]
              |            ]
              |          },
              |          {
              |            "type": "uint",
              |            "value": 10
              |          }
              |        ]
              |      ]
              |    }
              |  ]
              |}
            """.trimMargin()

        verify("must correctly parse array of nested tuples with arrays") {
            val result = objectMapper.readValue(json, Result::class.java).args
                .map {
                    @Suppress("UNCHECKED_CAST")
                    it.value as DynamicArray<DynamicStruct>
                }
                .map {
                    it.value.map { tuple ->
                        tuple.value.map { tupleElem ->
                            if (tupleElem is DynamicArray<*>) {
                                @Suppress("UNCHECKED_CAST")
                                val innerTupleArray = tupleElem as DynamicArray<DynamicStruct>

                                innerTupleArray.value.map { innerTuple ->
                                    innerTuple.value.map { innerTupleElem ->
                                        if (innerTupleElem is StaticStruct) {
                                            innerTupleElem.value.map { v -> v.value }
                                        } else {
                                            innerTupleElem.value
                                        }
                                    }
                                }
                            } else if (tupleElem is SizedStaticArray<*>) {
                                tupleElem.value.map { e ->
                                    @Suppress("UNCHECKED_CAST")
                                    (e as DynamicArray<Utf8String>).value.map { s -> s.value }
                                }
                            } else {
                                tupleElem.value
                            }
                        }
                    }
                }[0]

            expectThat(result)
                .isEqualTo(
                    listOf( // tuple array
                        listOf( // tuple 1
                            listOf(listOf("test1")),
                            listOf( // inner tuple array 1
                                listOf( // inner tuple 1
                                    listOf(Address("0x0")),
                                    listOf( // innermost tuple 1
                                        BigInteger.TWO
                                    ),
                                    true,
                                    emptyList<Nothing>() // empty tuple array
                                )
                            ),
                            BigInteger.ONE
                        ),
                        listOf( // tuple 2
                            listOf(listOf("test2")),
                            listOf( // inner tuple array 2
                                listOf( // inner tuple 2
                                    listOf(Address("0x1")),
                                    listOf( // innermost tuple 2
                                        BigInteger.ZERO
                                    ),
                                    false,
                                    emptyList<Nothing>() // empty tuple array
                                )
                            ),
                            BigInteger.TEN
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyGetTypeHierarchy() {
        val deserializer = FunctionArgumentJsonDeserializer()
        val simpleJson =
            """{
              |  "type": "string",
              |  "value": "test"
              |}
            """.trimMargin()

        verify("correct type hierarchy is returned for simple JSON") {
            val tree = objectMapper.readTree(simpleJson)

            expectThat(deserializer.getTypeHierarchy(tree))
                .isEqualTo("string")
        }

        val arrayJson =
            """{
              |  "type": "string[]",
              |  "value": ["test"]
              |}
            """.trimMargin()

        verify("correct type hierarchy is returned for array JSON") {
            val tree = objectMapper.readTree(arrayJson)

            expectThat(deserializer.getTypeHierarchy(tree))
                .isEqualTo("string[]")
        }

        val emptyArrayJson =
            """{
              |  "type": "string[0]",
              |  "value": []
              |}
            """.trimMargin()

        verify("correct type hierarchy is returned for array JSON") {
            val tree = objectMapper.readTree(emptyArrayJson)

            expectThat(deserializer.getTypeHierarchy(tree))
                .isEqualTo("string[0]")
        }

        val nestedArrayJson =
            """{
              |  "type": "string[1][1][]",
              |  "value": [[["test"]]]
              |}
            """.trimMargin()

        verify("correct type hierarchy is returned for nested array JSON") {
            val tree = objectMapper.readTree(nestedArrayJson)

            expectThat(deserializer.getTypeHierarchy(tree))
                .isEqualTo("string[1][1][]")
        }

        val tupleJson =
            """{
              |  "type": "tuple",
              |  "value": [
              |    {
              |      "type": "string",
              |      "value": "test"
              |    },
              |    {
              |      "type": "uint",
              |      "value": 1
              |    }
              |  ]
              |}
            """.trimMargin()

        verify("correct type hierarchy is returned for tuple JSON") {
            val tree = objectMapper.readTree(tupleJson)

            expectThat(deserializer.getTypeHierarchy(tree))
                .isEqualTo("tuple(string,uint)")
        }

        val nestedTupleJson =
            """{
              |  "type": "tuple",
              |  "value": [
              |    {
              |      "type": "string",
              |      "value": "test"
              |    },
              |    {
              |      "type": "tuple",
              |      "value": [
              |        {
              |          "type": "address",
              |          "value": "0x0"
              |        },
              |        {
              |          "type": "tuple",
              |          "value": [
              |            {
              |              "type": "int",
              |              "value": 2
              |            }
              |          ]
              |        },
              |        {
              |          "type": "bool",
              |          "value": true
              |        }
              |      ]
              |    },
              |    {
              |      "type": "uint",
              |      "value": 1
              |    }
              |  ]
              |}
            """.trimMargin()

        verify("correct type hierarchy is returned for tuple JSON") {
            val tree = objectMapper.readTree(nestedTupleJson)

            expectThat(deserializer.getTypeHierarchy(tree))
                .isEqualTo("tuple(string,tuple(address,tuple(int),bool),uint)")
        }

        val emptyTupleArrayJson =
            """{
              |  "type": "tuple[]",
              |  "value": []
              |}
            """.trimMargin()

        verify("correct type hierarchy is returned for empty tuple array JSON") {
            val tree = objectMapper.readTree(emptyTupleArrayJson)

            expectThat(deserializer.getTypeHierarchy(tree))
                .isEqualTo("tuple(*)[]")
        }

        val emptyNestedTupleArrayJson =
            """{
              |  "type": "tuple[][0][]",
              |  "value": [[[]]]
              |}
            """.trimMargin()

        verify("correct type hierarchy is returned for nested empty tuple array JSON") {
            val tree = objectMapper.readTree(emptyNestedTupleArrayJson)

            expectThat(deserializer.getTypeHierarchy(tree))
                .isEqualTo("tuple(*)[][0][]")
        }

        val tupleArrayJson =
            """{
              |  "type": "tuple[]",
              |  "value": [
              |    [
              |      {
              |        "type": "string",
              |        "value": "test"
              |      },
              |      {
              |        "type": "uint",
              |        "value": 1
              |      }
              |    ]
              |  ]
              |}
            """.trimMargin()

        verify("correct type hierarchy is returned for tuple array JSON") {
            val tree = objectMapper.readTree(tupleArrayJson)

            expectThat(deserializer.getTypeHierarchy(tree))
                .isEqualTo("tuple(string,uint)[]")
        }

        val nestedTupleArrayJson =
            """{
              |  "type": "tuple[1][1][]",
              |  "value": [
              |    [
              |      [
              |        [
              |          {
              |            "type": "string",
              |            "value": "test"
              |          },
              |          {
              |            "type": "uint",
              |            "value": 1
              |          }
              |        ]
              |      ]
              |    ]
              |  ]
              |}
            """.trimMargin()

        verify("correct type hierarchy is returned for nested tuple array JSON") {
            val tree = objectMapper.readTree(nestedTupleArrayJson)

            expectThat(deserializer.getTypeHierarchy(tree))
                .isEqualTo("tuple(string,uint)[1][1][]")
        }

        val nestedTupleWithArraysJson =
            """{
              |  "type": "tuple[]",
              |  "value": [
              |    [
              |      {
              |        "type": "string[][1]",
              |        "value": [["test"]]
              |      },
              |      {
              |        "type": "tuple[]",
              |        "value": [
              |          [
              |            {
              |              "type": "address[]",
              |              "value": ["0x0"]
              |            },
              |            {
              |              "type": "tuple",
              |              "value": [
              |                {
              |                  "type": "int",
              |                  "value": 2
              |                }
              |              ]
              |            },
              |            {
              |              "type": "bool",
              |              "value": true
              |            },
              |            {
              |              "type": "tuple[]",
              |              "value": []
              |            }
              |          ]
              |        ]
              |      },
              |      {
              |        "type": "uint",
              |        "value": 1
              |      }
              |    ]
              |  ]
              |}
            """.trimMargin()

        verify("correct type hierarchy is returned for tuple with arrays JSON") {
            val tree = objectMapper.readTree(nestedTupleWithArraysJson)

            expectThat(deserializer.getTypeHierarchy(tree))
                .isEqualTo("tuple(string[][1],tuple(address[],tuple(int),bool,tuple(*)[])[],uint)[]")
        }
    }

    @Test
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseWhenDeserializedValueIsNotAnObject() {
        val json =
            """{
              |  "args": [
              |    "invalid-arg"
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
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseWhenTypeIsMissing() {
        val json =
            """{
              |  "args": [
              |    {
              |       "value": 123
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
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseWhenValueIsMissing() {
        val json =
            """{
              |  "args": [
              |    {
              |       "type": "address"
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
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseForUnknownType() {
        val json =
            """{
              |  "args": [
              |    {
              |       "type": "dummy-type",
              |       "value": 123
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
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseWhenParsingTextFails() {
        val json =
            """{
              |  "args": [
              |    {
              |       "type": "address",
              |       "value": 123
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
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseWhenParsingBooleanFails() {
        val json =
            """{
              |  "args": [
              |    {
              |       "type": "bool",
              |       "value": 123
              |    }
              |  ]
              |
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
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseWhenParsingBigIntFails() {
        val json =
            """{
              |  "args": [
              |    {
              |       "type": "uint",
              |       "value": {}
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
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseWhenParsingBytesFails() {
        val json =
            """{
              |  "args": [
              |    {
              |       "type": "bytes",
              |       "value": "not-an-array"
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
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseWhenParsingBytesHaveWrongLength() {
        val json =
            """{
              |  "args": [
              |    {
              |       "type": "bytes1",
              |       "value": [1, 2]
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
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseForUnknownArrayType() {
        val json =
            """{
              |  "args": [
              |    {
              |      "type": "dummy-type[]",
              |      "value": []
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
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseWhenArrayValueIsNotAnArray() {
        val json =
            """{
              |  "args": [
              |    {
              |      "type": "uint[]",
              |      "value": false
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
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseWhenArrayHasInconsistentElements() {
        val json =
            """{
              |  "args": [
              |    {
              |      "type": "uint[]",
              |      "value": [1, false]
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
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseWhenSizedArrayHasInvalidNumberOfElements() {
        val json =
            """{
              |  "args": [
              |    {
              |      "type": "uint[2]",
              |      "value": [1]
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
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseWhenParsingEmptyTuple() {
        val json =
            """{
              |  "args": [
              |    {
              |      "type": "tuple",
              |      "value": []
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
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseWhenParsingTupleWithNonArrayValue() {
        val json =
            """{
              |  "args": [
              |    {
              |      "type": "tuple",
              |      "value": "non-an-array"
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
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseWhenParsingTupleWithMissingElementType() {
        val json =
            """{
              |  "args": [
              |    {
              |      "type": "tuple",
              |      "value": [
              |        {
              |          "value": "missing-type"
              |        }
              |      ]
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
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseWhenParsingTupleWithMissingElementValue() {
        val json =
            """{
              |  "args": [
              |    {
              |      "type": "tuple",
              |      "value": [
              |        {
              |          "type": "missing-value"
              |        }
              |      ]
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
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseWhenParsingTupleArrayWithMismatchingTuples() {
        val json =
            """{
              |  "args": [
              |    {
              |      "type": "tuple[]",
              |      "value": [
              |        [
              |          {
              |            "type": "string",
              |            "value": "tuple1"
              |          },
              |          {
              |            "type": "uint",
              |            "value": 0
              |          }
              |        ],
              |        [
              |          {
              |            "type": "string",
              |            "value": "tuple2"
              |          }
              |        ]
              |      ]
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
    fun mustThrowJsonMappingExceptionWithJsonParseExceptionCauseWhenParsingTupleArrayWithMismatchingNestedTuples() {
        val json =
            """{
              |  "args": [
              |    {
              |      "type": "tuple[]",
              |      "value": [
              |        [
              |          {
              |            "type": "string[][1]",
              |            "value": [["test1"]]
              |          },
              |          {
              |            "type": "tuple[]",
              |            "value": [
              |              [
              |                {
              |                  "type": "address[]",
              |                  "value": ["0x0"]
              |                },
              |                {
              |                  "type": "tuple",
              |                  "value": [
              |                    {
              |                      "type": "int",
              |                      "value": 2
              |                    }
              |                  ]
              |                },
              |                {
              |                  "type": "bool",
              |                  "value": true
              |                },
              |                {
              |                  "type": "tuple[]",
              |                  "value": []
              |                }
              |              ]
              |            ]
              |          },
              |          {
              |            "type": "uint",
              |            "value": 1
              |          }
              |        ],
              |        [
              |          {
              |            "type": "string[][1]",
              |            "value": [["test2"]]
              |          },
              |          {
              |            "type": "tuple[]",
              |            "value": [
              |              [
              |                {
              |                  "type": "address[]",
              |                  "value": ["0x1"]
              |                },
              |                {
              |                  "type": "tuple",
              |                  "value": [
              |                    {
              |                      "type": "int",
              |                      "value": 0
              |                    }
              |                  ]
              |                },
              |                {
              |                  "type": "bool",
              |                  "value": false
              |                },
              |                {
              |                  "type": "tuple[]",
              |                  "value": [
              |                    [
              |                      {
              |                        "type": "string",
              |                        "value": "non-matching"
              |                      }
              |                    ]
              |                  ]
              |                }
              |              ]
              |            ]
              |          },
              |          {
              |            "type": "uint",
              |            "value": 10
              |          }
              |        ]
              |      ]
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
