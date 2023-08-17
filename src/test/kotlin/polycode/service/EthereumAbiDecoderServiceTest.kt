package polycode.service

import org.assertj.core.api.ListAssert
import org.junit.jupiter.api.Test
import polycode.TestBase
import polycode.features.contract.abi.model.AbiType
import polycode.features.contract.abi.model.AddressType
import polycode.features.contract.abi.model.BoolType
import polycode.features.contract.abi.model.DynamicArrayType
import polycode.features.contract.abi.model.DynamicBytesType
import polycode.features.contract.abi.model.IntType
import polycode.features.contract.abi.model.StaticArrayType
import polycode.features.contract.abi.model.StaticBytesType
import polycode.features.contract.abi.model.StringType
import polycode.features.contract.abi.model.Tuple
import polycode.features.contract.abi.model.TupleType
import polycode.features.contract.abi.model.UintType
import polycode.features.contract.abi.service.AbiDecoderService
import polycode.features.contract.abi.service.EthereumAbiDecoderService
import java.math.BigInteger

class EthereumAbiDecoderServiceTest : TestBase() {

    private val decoder: AbiDecoderService = EthereumAbiDecoderService()

    @Test
    fun mustCorrectlyDecodeStaticBaseTypes() {
        verify("uint type is correctly decoded") {
            decoding(listOf(UintType), "0x000000000000000000000000000000000000000000000000000000000000007f")
                .returns(BigInteger.valueOf(127L))
        }

        verify("positive int type is correctly decoded") {
            decoding(listOf(IntType), "0x000000000000000000000000000000000000000000000000000000000000007f")
                .returns(BigInteger.valueOf(127L))
        }

        verify("negative int type is correctly decoded") {
            decoding(listOf(IntType), "0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff81")
                .returns(BigInteger.valueOf(-127L))
        }

        verify("static bytes are correctly decoded") {
            decoding(listOf(StaticBytesType(1)), "0x3100000000000000000000000000000000000000000000000000000000000000")
                .returns("1".byteList())
            decoding(listOf(StaticBytesType(2)), "0x3132000000000000000000000000000000000000000000000000000000000000")
                .returns("12".byteList())
            decoding(listOf(StaticBytesType(3)), "0x3132330000000000000000000000000000000000000000000000000000000000")
                .returns("123".byteList())
            decoding(listOf(StaticBytesType(4)), "0x3132333400000000000000000000000000000000000000000000000000000000")
                .returns("1234".byteList())
            decoding(listOf(StaticBytesType(5)), "0x3132333435000000000000000000000000000000000000000000000000000000")
                .returns("12345".byteList())
            decoding(listOf(StaticBytesType(6)), "0x3132333435360000000000000000000000000000000000000000000000000000")
                .returns("123456".byteList())
            decoding(listOf(StaticBytesType(7)), "0x3132333435363700000000000000000000000000000000000000000000000000")
                .returns("1234567".byteList())
            decoding(listOf(StaticBytesType(8)), "0x3132333435363738000000000000000000000000000000000000000000000000")
                .returns("12345678".byteList())
            decoding(listOf(StaticBytesType(9)), "0x3132333435363738390000000000000000000000000000000000000000000000")
                .returns("123456789".byteList())
            decoding(listOf(StaticBytesType(10)), "0x3132333435363738393000000000000000000000000000000000000000000000")
                .returns("1234567890".byteList())
            decoding(listOf(StaticBytesType(11)), "0x3132333435363738393031000000000000000000000000000000000000000000")
                .returns("12345678901".byteList())
            decoding(listOf(StaticBytesType(12)), "0x3132333435363738393031320000000000000000000000000000000000000000")
                .returns("123456789012".byteList())
            decoding(listOf(StaticBytesType(13)), "0x3132333435363738393031323300000000000000000000000000000000000000")
                .returns("1234567890123".byteList())
            decoding(listOf(StaticBytesType(14)), "0x3132333435363738393031323334000000000000000000000000000000000000")
                .returns("12345678901234".byteList())
            decoding(listOf(StaticBytesType(15)), "0x3132333435363738393031323334350000000000000000000000000000000000")
                .returns("123456789012345".byteList())
            decoding(listOf(StaticBytesType(16)), "0x3132333435363738393031323334353600000000000000000000000000000000")
                .returns("1234567890123456".byteList())
            decoding(listOf(StaticBytesType(17)), "0x3132333435363738393031323334353637000000000000000000000000000000")
                .returns("12345678901234567".byteList())
            decoding(listOf(StaticBytesType(18)), "0x3132333435363738393031323334353637380000000000000000000000000000")
                .returns("123456789012345678".byteList())
            decoding(listOf(StaticBytesType(19)), "0x3132333435363738393031323334353637383900000000000000000000000000")
                .returns("1234567890123456789".byteList())
            decoding(listOf(StaticBytesType(20)), "0x3132333435363738393031323334353637383930000000000000000000000000")
                .returns("12345678901234567890".byteList())
            decoding(listOf(StaticBytesType(21)), "0x3132333435363738393031323334353637383930310000000000000000000000")
                .returns("123456789012345678901".byteList())
            decoding(listOf(StaticBytesType(22)), "0x3132333435363738393031323334353637383930313200000000000000000000")
                .returns("1234567890123456789012".byteList())
            decoding(listOf(StaticBytesType(23)), "0x3132333435363738393031323334353637383930313233000000000000000000")
                .returns("12345678901234567890123".byteList())
            decoding(listOf(StaticBytesType(24)), "0x3132333435363738393031323334353637383930313233340000000000000000")
                .returns("123456789012345678901234".byteList())
            decoding(listOf(StaticBytesType(25)), "0x3132333435363738393031323334353637383930313233343500000000000000")
                .returns("1234567890123456789012345".byteList())
            decoding(listOf(StaticBytesType(26)), "0x3132333435363738393031323334353637383930313233343536000000000000")
                .returns("12345678901234567890123456".byteList())
            decoding(listOf(StaticBytesType(27)), "0x3132333435363738393031323334353637383930313233343536370000000000")
                .returns("123456789012345678901234567".byteList())
            decoding(listOf(StaticBytesType(28)), "0x3132333435363738393031323334353637383930313233343536373800000000")
                .returns("1234567890123456789012345678".byteList())
            decoding(listOf(StaticBytesType(29)), "0x3132333435363738393031323334353637383930313233343536373839000000")
                .returns("12345678901234567890123456789".byteList())
            decoding(listOf(StaticBytesType(30)), "0x3132333435363738393031323334353637383930313233343536373839300000")
                .returns("123456789012345678901234567890".byteList())
            decoding(listOf(StaticBytesType(31)), "0x3132333435363738393031323334353637383930313233343536373839303100")
                .returns("1234567890123456789012345678901".byteList())
            decoding(listOf(StaticBytesType(32)), "0x3132333435363738393031323334353637383930313233343536373839303132")
                .returns("12345678901234567890123456789012".byteList())
        }

        verify("address is correctly decoded") {
            decoding(listOf(AddressType), "0x00000000000000000000000000000000000000000000000000000000000000af")
                .returns("0x00000000000000000000000000000000000000af")
        }

        verify("bool is correctly decoded") {
            decoding(listOf(BoolType), "0x0000000000000000000000000000000000000000000000000000000000000001")
                .returns(true)
        }
    }

    @Test
    fun mustCorrectlyDecodeDynamicBaseTypes() {
        verify("short string type is correctly decoded") {
            decoding(
                listOf(StringType),
                "0x" +
                    "0000000000000000000000000000000000000000000000000000000000000020" + // offset
                    "000000000000000000000000000000000000000000000000000000000000000b" + // length
                    "746573745f737472696e67000000000000000000000000000000000000000000" // value
            )
                .returns("test_string")
        }

        verify("long string type is correctly decoded") {
            decoding(
                listOf(StringType),
                "0x" +
                    "0000000000000000000000000000000000000000000000000000000000000020" + // offset
                    "0000000000000000000000000000000000000000000000000000000000000041" + // length
                    "736f6d655f6c6f6e675f737472696e675f76616c75655f77686963685f646f65" + // value
                    "735f6e6f745f6669745f696e5f626173655f7061646465645f33325f62797465" +
                    "7300000000000000000000000000000000000000000000000000000000000000"
            )
                .returns("some_long_string_value_which_does_not_fit_in_base_padded_32_bytes")
        }

        verify("short bytes type is correctly decoded") {
            decoding(
                listOf(DynamicBytesType),
                "0x" +
                    "0000000000000000000000000000000000000000000000000000000000000020" + // offset
                    "000000000000000000000000000000000000000000000000000000000000000a" + // length
                    "746573745f627974657300000000000000000000000000000000000000000000" // value
            )
                .returns("test_bytes".byteList())
        }

        verify("long bytes type is correctly decoded") {
            decoding(
                listOf(DynamicBytesType),
                "0x" +
                    "0000000000000000000000000000000000000000000000000000000000000020" + // offset
                    "0000000000000000000000000000000000000000000000000000000000000040" + // length
                    "736f6d655f6c6f6e675f62797465735f76616c75655f77686963685f646f6573" + // value
                    "5f6e6f745f6669745f696e5f626173655f7061646465645f33325f6279746573"
            )
                .returns("some_long_bytes_value_which_does_not_fit_in_base_padded_32_bytes".byteList())
        }
    }

    @Test
    fun mustCorrectlyDecodeSizedArrays() {
        verify("uint[3] array is correctly decoded") {
            decoding(
                listOf(StaticArrayType(UintType, 3)),
                "0x" +
                    "0000000000000000000000000000000000000000000000000000000000000064" + // value 1
                    "00000000000000000000000000000000000000000000000000000000000000c8" + // value 2
                    "000000000000000000000000000000000000000000000000000000000000012c" // value 3
            )
                .returns(listOf(BigInteger.valueOf(100L), BigInteger.valueOf(200L), BigInteger.valueOf(300L)))
        }

        verify("string[2] array is correctly decoded") {
            decoding(
                listOf(StaticArrayType(StringType, 2)),
                "0x" +
                    "0000000000000000000000000000000000000000000000000000000000000020" + // array offset
                    "0000000000000000000000000000000000000000000000000000000000000040" + // str1 offset
                    "0000000000000000000000000000000000000000000000000000000000000080" + // str2 offset
                    "0000000000000000000000000000000000000000000000000000000000000004" + // str1 length
                    "7374723100000000000000000000000000000000000000000000000000000000" + // str1 value
                    "0000000000000000000000000000000000000000000000000000000000000004" + // str2 length
                    "7374723200000000000000000000000000000000000000000000000000000000" // str2 value
            )
                .returns(listOf("str1", "str2"))
        }
    }

    @Test
    fun mustCorrectlyDecodeDynamicArrays() {
        verify("uint[] array is correctly decoded") {
            decoding(
                listOf(DynamicArrayType(UintType)),
                "0x" +
                    "0000000000000000000000000000000000000000000000000000000000000020" + // offset
                    "0000000000000000000000000000000000000000000000000000000000000005" + // length
                    "0000000000000000000000000000000000000000000000000000000000000032" + // values
                    "0000000000000000000000000000000000000000000000000000000000000064" +
                    "0000000000000000000000000000000000000000000000000000000000000096" +
                    "00000000000000000000000000000000000000000000000000000000000000c8" +
                    "00000000000000000000000000000000000000000000000000000000000000fa"
            )
                .returns(
                    listOf(
                        BigInteger.valueOf(50L),
                        BigInteger.valueOf(100L),
                        BigInteger.valueOf(150L),
                        BigInteger.valueOf(200L),
                        BigInteger.valueOf(250L)
                    )
                )
        }

        verify("string[] array is correctly decoded") {
            decoding(
                listOf(DynamicArrayType(StringType)),
                "0x" +
                    "0000000000000000000000000000000000000000000000000000000000000020" + // array offset
                    "0000000000000000000000000000000000000000000000000000000000000003" + // array length
                    "0000000000000000000000000000000000000000000000000000000000000060" + // str1 offset
                    "00000000000000000000000000000000000000000000000000000000000000a0" + // str2 offset
                    "00000000000000000000000000000000000000000000000000000000000000e0" + // str3 offset
                    "0000000000000000000000000000000000000000000000000000000000000004" + // str1 length
                    "7374723100000000000000000000000000000000000000000000000000000000" + // str1 value
                    "0000000000000000000000000000000000000000000000000000000000000004" + // str2 length
                    "7374723200000000000000000000000000000000000000000000000000000000" + // str2 value
                    "0000000000000000000000000000000000000000000000000000000000000004" + // str3 length
                    "7374723300000000000000000000000000000000000000000000000000000000" // str3 value
            )
                .returns(listOf("str1", "str2", "str3"))
        }
    }

    @Test
    fun mustCorrectlyDecodeNestedSizedArrays() {
        verify("uint[2][1] array is correctly decoded") {
            decoding(
                listOf(StaticArrayType(StaticArrayType(UintType, 2), 1)),
                "0x" +
                    "000000000000000000000000000000000000000000000000000000000000000a" + // value 1
                    "0000000000000000000000000000000000000000000000000000000000000014" // value 2
            )
                .returns(
                    listOf(
                        listOf(
                            BigInteger.valueOf(10L),
                            BigInteger.valueOf(20L)
                        )
                    )
                )
        }

        verify("string[3][2] array is correctly decoded") {
            decoding(
                listOf(StaticArrayType(StaticArrayType(StringType, 3), 2)),
                "0x" +
                    "0000000000000000000000000000000000000000000000000000000000000020" + // outer array offset
                    "0000000000000000000000000000000000000000000000000000000000000040" + // array1 offset
                    "0000000000000000000000000000000000000000000000000000000000000160" + // array2 offset
                    "0000000000000000000000000000000000000000000000000000000000000060" + // str1 offset
                    "00000000000000000000000000000000000000000000000000000000000000a0" + // str2 offset
                    "00000000000000000000000000000000000000000000000000000000000000e0" + // str3 offset
                    "0000000000000000000000000000000000000000000000000000000000000004" + // str1 length
                    "7374723100000000000000000000000000000000000000000000000000000000" + // str1 value
                    "0000000000000000000000000000000000000000000000000000000000000004" + // str2 length
                    "7374723200000000000000000000000000000000000000000000000000000000" + // str2 value
                    "0000000000000000000000000000000000000000000000000000000000000004" + // str3 offset
                    "7374723300000000000000000000000000000000000000000000000000000000" + // str3 value
                    "0000000000000000000000000000000000000000000000000000000000000060" + // str4 offset
                    "00000000000000000000000000000000000000000000000000000000000000a0" + // str5 offset
                    "00000000000000000000000000000000000000000000000000000000000000e0" + // str6 offset
                    "0000000000000000000000000000000000000000000000000000000000000004" + // str4 length
                    "7374723400000000000000000000000000000000000000000000000000000000" + // str4 value
                    "0000000000000000000000000000000000000000000000000000000000000004" + // str5 length
                    "7374723500000000000000000000000000000000000000000000000000000000" + // str5 value
                    "0000000000000000000000000000000000000000000000000000000000000004" + // str6 length
                    "7374723600000000000000000000000000000000000000000000000000000000", // str6 value
            )
                .returns(
                    listOf(
                        listOf("str1", "str2", "str3"),
                        listOf("str4", "str5", "str6")
                    )
                )
        }

        verify("uint[][1][2] array is correctly decoded") {
            decoding(
                listOf(StaticArrayType(StaticArrayType(DynamicArrayType(UintType), 1), 2)),
                "0x" +
                    "0000000000000000000000000000000000000000000000000000000000000020" + // outer array offset
                    "0000000000000000000000000000000000000000000000000000000000000040" + // array1 offset
                    "00000000000000000000000000000000000000000000000000000000000000c0" + // array2 offset
                    "0000000000000000000000000000000000000000000000000000000000000020" + // dynamic array1 offset
                    "0000000000000000000000000000000000000000000000000000000000000002" + // dynamic array1 length
                    "0000000000000000000000000000000000000000000000000000000000000032" + // value 50
                    "0000000000000000000000000000000000000000000000000000000000000064" + // value 100
                    "0000000000000000000000000000000000000000000000000000000000000020" + // dynamic array2 offset
                    "0000000000000000000000000000000000000000000000000000000000000003" + // dynamic array2 length
                    "0000000000000000000000000000000000000000000000000000000000000096" + // value 150
                    "00000000000000000000000000000000000000000000000000000000000000c8" + // value 200
                    "00000000000000000000000000000000000000000000000000000000000000fa" // value 250
            )
                .returns(
                    listOf(
                        listOf(
                            listOf(
                                BigInteger.valueOf(50L),
                                BigInteger.valueOf(100L)
                            )
                        ),
                        listOf(
                            listOf(
                                BigInteger.valueOf(150L),
                                BigInteger.valueOf(200L),
                                BigInteger.valueOf(250L)
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyDecodeNestedDynamicArrays() {
        verify("uint[][] array is correctly decoded") {
            decoding(
                listOf(DynamicArrayType(DynamicArrayType(UintType))),
                "0x" +
                    "0000000000000000000000000000000000000000000000000000000000000020" + // outer array offset
                    "0000000000000000000000000000000000000000000000000000000000000001" + // outer array length
                    "0000000000000000000000000000000000000000000000000000000000000020" + // inner array offset
                    "0000000000000000000000000000000000000000000000000000000000000002" + // inner array length
                    "000000000000000000000000000000000000000000000000000000000000000a" + // value 10
                    "0000000000000000000000000000000000000000000000000000000000000014" // value 20
            )
                .returns(
                    listOf(
                        listOf(
                            BigInteger.valueOf(10L),
                            BigInteger.valueOf(20L)
                        )
                    )
                )
        }

        verify("uint[5][] array is correctly decoded") {
            decoding(
                listOf(DynamicArrayType(StaticArrayType(UintType, 5))),
                "0x" +
                    "0000000000000000000000000000000000000000000000000000000000000020" + // dynamic array offset
                    "0000000000000000000000000000000000000000000000000000000000000001" + // dynamic array length
                    "0000000000000000000000000000000000000000000000000000000000000064" + // value 100
                    "00000000000000000000000000000000000000000000000000000000000000c8" + // value 200
                    "000000000000000000000000000000000000000000000000000000000000012c" + // value 300
                    "0000000000000000000000000000000000000000000000000000000000000190" + // value 400
                    "00000000000000000000000000000000000000000000000000000000000001f4" // value 500
            )
                .returns(
                    listOf(
                        listOf(
                            BigInteger.valueOf(100L),
                            BigInteger.valueOf(200L),
                            BigInteger.valueOf(300L),
                            BigInteger.valueOf(400L),
                            BigInteger.valueOf(500L)
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyDecodeStaticTypeList() {
        verify("(uint, bool, bytes5) tuple is correctly decoded") {
            decoding(
                listOf(UintType, BoolType, StaticBytesType(5)),
                "0x" +
                    "0000000000000000000000000000000000000000000000000000000000000064" + // uint
                    "0000000000000000000000000000000000000000000000000000000000000000" + // bool
                    "3132333435000000000000000000000000000000000000000000000000000000" // bytes5
            )
                .returns(
                    BigInteger.valueOf(100L),
                    false,
                    "12345".byteList()
                )
        }
    }

    @Test
    fun mustCorrectlyDecodeDynamicTypeList() {
        verify("(uint, uint[2], string, bool, uint[]) tuple is correctly decoded") {
            decoding(
                listOf(UintType, StaticArrayType(UintType, 2), StringType, BoolType, DynamicArrayType(UintType)),
                "0x" +
                    "0000000000000000000000000000000000000000000000000000000000000064" + // value 100
                    "00000000000000000000000000000000000000000000000000000000000000c8" + // value 200
                    "000000000000000000000000000000000000000000000000000000000000012c" + // value 300
                    "00000000000000000000000000000000000000000000000000000000000000c0" + // string offset
                    "0000000000000000000000000000000000000000000000000000000000000001" + // value true
                    "0000000000000000000000000000000000000000000000000000000000000100" + // array offset
                    "0000000000000000000000000000000000000000000000000000000000000004" + // string length
                    "7465737400000000000000000000000000000000000000000000000000000000" + // value "test"
                    "0000000000000000000000000000000000000000000000000000000000000003" + // array length
                    "0000000000000000000000000000000000000000000000000000000000000190" + // value 400
                    "00000000000000000000000000000000000000000000000000000000000001f4" + // value 500
                    "0000000000000000000000000000000000000000000000000000000000000258" // value 600
            )
                .returns(
                    BigInteger.valueOf(100L),
                    listOf(
                        BigInteger.valueOf(200L),
                        BigInteger.valueOf(300L)
                    ),
                    "test",
                    true,
                    listOf(
                        BigInteger.valueOf(400L),
                        BigInteger.valueOf(500L),
                        BigInteger.valueOf(600L)
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyDecodeStaticArrayTypeList() {
        verify("(uint[2][3], bool[1][2][3], int[2][1]) tuple is correctly decoded") {
            decoding(
                listOf(
                    StaticArrayType(StaticArrayType(UintType, 2), 3),
                    StaticArrayType(StaticArrayType(StaticArrayType(BoolType, 1), 2), 3),
                    StaticArrayType(StaticArrayType(IntType, 2), 1)
                ),
                "0x" +
                    "0000000000000000000000000000000000000000000000000000000000000064" + // 100
                    "00000000000000000000000000000000000000000000000000000000000000c8" + // 200
                    "000000000000000000000000000000000000000000000000000000000000012c" + // 300
                    "0000000000000000000000000000000000000000000000000000000000000190" + // 400
                    "00000000000000000000000000000000000000000000000000000000000001f4" + // 500
                    "0000000000000000000000000000000000000000000000000000000000000258" + // 600
                    "0000000000000000000000000000000000000000000000000000000000000001" + // true
                    "0000000000000000000000000000000000000000000000000000000000000000" + // false
                    "0000000000000000000000000000000000000000000000000000000000000000" + // false
                    "0000000000000000000000000000000000000000000000000000000000000001" + // true
                    "0000000000000000000000000000000000000000000000000000000000000001" + // true
                    "0000000000000000000000000000000000000000000000000000000000000001" + // true
                    "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffd44" + // -700
                    "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffce0" // -800
            )
                .returns(
                    listOf(
                        listOf(BigInteger.valueOf(100L), BigInteger.valueOf(200L)),
                        listOf(BigInteger.valueOf(300L), BigInteger.valueOf(400L)),
                        listOf(BigInteger.valueOf(500L), BigInteger.valueOf(600L))
                    ),
                    listOf(
                        listOf(
                            listOf(true),
                            listOf(false)
                        ),
                        listOf(
                            listOf(false),
                            listOf(true)
                        ),
                        listOf(
                            listOf(true),
                            listOf(true)
                        )
                    ),
                    listOf(
                        listOf(BigInteger.valueOf(-700), BigInteger.valueOf(-800))
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyDecodeStaticTuple() {
        verify("tuple(uint, bool, bytes5) is correctly decoded") {
            decoding(
                listOf(TupleType(UintType, BoolType, StaticBytesType(5))),
                "0x" +
                    "0000000000000000000000000000000000000000000000000000000000000064" + // uint
                    "0000000000000000000000000000000000000000000000000000000000000000" + // bool
                    "3132333435000000000000000000000000000000000000000000000000000000" // bytes5
            )
                .returns(
                    tupleOf(
                        BigInteger.valueOf(100L),
                        false,
                        "12345".byteList()
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyDecodeDynamicTuple() {
        verify("tuple(uint, uint[2], string, bool, uint[]) is correctly decoded") {
            decoding(
                listOf(
                    TupleType(UintType, StaticArrayType(UintType, 2), StringType, BoolType, DynamicArrayType(UintType))
                ),
                "0x" +
                    "0000000000000000000000000000000000000000000000000000000000000020" + // tuple offset
                    "0000000000000000000000000000000000000000000000000000000000000064" + // value 100
                    "00000000000000000000000000000000000000000000000000000000000000c8" + // value 200
                    "000000000000000000000000000000000000000000000000000000000000012c" + // value 300
                    "00000000000000000000000000000000000000000000000000000000000000c0" + // string offset
                    "0000000000000000000000000000000000000000000000000000000000000001" + // value true
                    "0000000000000000000000000000000000000000000000000000000000000100" + // array offset
                    "0000000000000000000000000000000000000000000000000000000000000004" + // string length
                    "7465737400000000000000000000000000000000000000000000000000000000" + // value "test"
                    "0000000000000000000000000000000000000000000000000000000000000003" + // array length
                    "0000000000000000000000000000000000000000000000000000000000000190" + // value 400
                    "00000000000000000000000000000000000000000000000000000000000001f4" + // value 500
                    "0000000000000000000000000000000000000000000000000000000000000258" // value 600
            )
                .returns(
                    tupleOf(
                        BigInteger.valueOf(100L),
                        listOf(
                            BigInteger.valueOf(200L),
                            BigInteger.valueOf(300L)
                        ),
                        "test",
                        true,
                        listOf(
                            BigInteger.valueOf(400L),
                            BigInteger.valueOf(500L),
                            BigInteger.valueOf(600L)
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyDecodeStaticTupleArray() {
        verify("tuple(uint, bool, bytes5)[] array is correctly decoded") {
            decoding(
                listOf(DynamicArrayType(TupleType(UintType, BoolType, StaticBytesType(5)))),
                "0x" +
                    "0000000000000000000000000000000000000000000000000000000000000020" + // array offset
                    "0000000000000000000000000000000000000000000000000000000000000003" + // array length
                    "0000000000000000000000000000000000000000000000000000000000000064" + // value 100
                    "0000000000000000000000000000000000000000000000000000000000000001" + // value true
                    "3132333435000000000000000000000000000000000000000000000000000000" + // value "12345"
                    "00000000000000000000000000000000000000000000000000000000000000c8" + // value 200
                    "0000000000000000000000000000000000000000000000000000000000000000" + // value false
                    "3637383930000000000000000000000000000000000000000000000000000000" + // value "67890"
                    "000000000000000000000000000000000000000000000000000000000000012c" + // value 300
                    "0000000000000000000000000000000000000000000000000000000000000001" + // value true
                    "3132333435000000000000000000000000000000000000000000000000000000" // value "12345"
            )
                .returns(
                    listOf(
                        tupleOf(BigInteger.valueOf(100L), true, "12345".byteList()),
                        tupleOf(BigInteger.valueOf(200L), false, "67890".byteList()),
                        tupleOf(BigInteger.valueOf(300L), true, "12345".byteList())
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyDecodeStaticTupleArrayTypeList() {
        verify("(tuple(uint[2], bool, uint), uint[4], tuple(uint[2], bool, uint)[2], bool) is correctly decoded") {
            decoding(
                listOf(
                    TupleType(StaticArrayType(UintType, 2), BoolType, UintType),
                    StaticArrayType(UintType, 4),
                    StaticArrayType(TupleType(StaticArrayType(UintType, 2), BoolType, UintType), 2),
                    BoolType
                ),
                "0x" +
                    "0000000000000000000000000000000000000000000000000000000000000064" + // 100
                    "00000000000000000000000000000000000000000000000000000000000000c8" + // 200
                    "0000000000000000000000000000000000000000000000000000000000000001" + // true
                    "000000000000000000000000000000000000000000000000000000000000012c" + // 300
                    "0000000000000000000000000000000000000000000000000000000000000190" + // 400
                    "00000000000000000000000000000000000000000000000000000000000001f4" + // 500
                    "0000000000000000000000000000000000000000000000000000000000000258" + // 600
                    "00000000000000000000000000000000000000000000000000000000000002bc" + // 700
                    "0000000000000000000000000000000000000000000000000000000000000320" + // 800
                    "0000000000000000000000000000000000000000000000000000000000000384" + // 900
                    "0000000000000000000000000000000000000000000000000000000000000001" + // true
                    "00000000000000000000000000000000000000000000000000000000000003e8" + // 1000
                    "000000000000000000000000000000000000000000000000000000000000044c" + // 1100
                    "00000000000000000000000000000000000000000000000000000000000004b0" + // 1200
                    "0000000000000000000000000000000000000000000000000000000000000001" + // true
                    "0000000000000000000000000000000000000000000000000000000000000514" + // 1300
                    "0000000000000000000000000000000000000000000000000000000000000001" // true
            )
                .returns(
                    tupleOf(
                        listOf(BigInteger.valueOf(100L), BigInteger.valueOf(200L)),
                        true,
                        BigInteger.valueOf(300L)
                    ),
                    listOf(
                        BigInteger.valueOf(400L),
                        BigInteger.valueOf(500L),
                        BigInteger.valueOf(600L),
                        BigInteger.valueOf(700L)
                    ),
                    listOf(
                        tupleOf(
                            listOf(BigInteger.valueOf(800L), BigInteger.valueOf(900L)),
                            true,
                            BigInteger.valueOf(1000L)
                        ),
                        tupleOf(
                            listOf(BigInteger.valueOf(1100L), BigInteger.valueOf(1200L)),
                            true,
                            BigInteger.valueOf(1300L)
                        )
                    ),
                    true
                )
        }
    }

    @Test
    fun mustCorrectlyDecodeDynamicTupleArray() {
        verify("tuple(uint, uint[2], string, bool, uint[])[] array is correctly decoded") {
            decoding(
                listOf(
                    DynamicArrayType(
                        TupleType(
                            UintType,
                            StaticArrayType(UintType, 2),
                            StringType,
                            BoolType,
                            DynamicArrayType(UintType)
                        )
                    )
                ),
                "0x" +
                    "0000000000000000000000000000000000000000000000000000000000000020" + // array offset
                    "0000000000000000000000000000000000000000000000000000000000000002" + // array length
                    "0000000000000000000000000000000000000000000000000000000000000040" + // tuple1 offset
                    "00000000000000000000000000000000000000000000000000000000000001c0" + // tuple2 offset
                    "0000000000000000000000000000000000000000000000000000000000000064" + // value 100
                    "00000000000000000000000000000000000000000000000000000000000000c8" + // value 200
                    "000000000000000000000000000000000000000000000000000000000000012c" + // value 300
                    "00000000000000000000000000000000000000000000000000000000000000c0" + // tuple1 string offset
                    "0000000000000000000000000000000000000000000000000000000000000001" + // value true
                    "0000000000000000000000000000000000000000000000000000000000000100" + // tuple1 array offset
                    "0000000000000000000000000000000000000000000000000000000000000005" + // tuple1 string length
                    "7465737431000000000000000000000000000000000000000000000000000000" + // value "test1"
                    "0000000000000000000000000000000000000000000000000000000000000003" + // tuple1 array length
                    "0000000000000000000000000000000000000000000000000000000000000190" + // value 400
                    "00000000000000000000000000000000000000000000000000000000000001f4" + // value 500
                    "0000000000000000000000000000000000000000000000000000000000000258" + // value 600
                    "00000000000000000000000000000000000000000000000000000000000003e8" + // value 1000
                    "00000000000000000000000000000000000000000000000000000000000007d0" + // value 2000
                    "0000000000000000000000000000000000000000000000000000000000000bb8" + // value 3000
                    "00000000000000000000000000000000000000000000000000000000000000c0" + // tuple2 string offset
                    "0000000000000000000000000000000000000000000000000000000000000000" + // value false
                    "0000000000000000000000000000000000000000000000000000000000000100" + // tuple2 array offset
                    "0000000000000000000000000000000000000000000000000000000000000005" + // tuple2 string length
                    "7465737432000000000000000000000000000000000000000000000000000000" + // value "test2"
                    "0000000000000000000000000000000000000000000000000000000000000002" + // tuple2 array length
                    "0000000000000000000000000000000000000000000000000000000000000fa0" + // value 4000
                    "0000000000000000000000000000000000000000000000000000000000001388" // value 5000
            )
                .returns(
                    listOf(
                        tupleOf(
                            BigInteger.valueOf(100L),
                            listOf(
                                BigInteger.valueOf(200L),
                                BigInteger.valueOf(300L)
                            ),
                            "test1",
                            true,
                            listOf(
                                BigInteger.valueOf(400L),
                                BigInteger.valueOf(500L),
                                BigInteger.valueOf(600L)
                            )
                        ),
                        tupleOf(
                            BigInteger.valueOf(1000L),
                            listOf(
                                BigInteger.valueOf(2000L),
                                BigInteger.valueOf(3000L)
                            ),
                            "test2",
                            false,
                            listOf(
                                BigInteger.valueOf(4000L),
                                BigInteger.valueOf(5000L)
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyDecodeNestedTuple() {
        verify("tuple(uint, tuple(uint[], tuple(uint, bool), string[]), bool, string) is correctly decoded") {
            decoding(
                listOf(
                    TupleType(
                        UintType,
                        TupleType(
                            DynamicArrayType(UintType),
                            TupleType(UintType, BoolType),
                            DynamicArrayType(StringType)
                        ),
                        BoolType,
                        StringType
                    )
                ),
                "0x" +
                    "0000000000000000000000000000000000000000000000000000000000000020" + // outer tuple offset
                    "00000000000000000000000000000000000000000000000000000000000001f4" + // value 500
                    "0000000000000000000000000000000000000000000000000000000000000080" + // inner tuple offset
                    "0000000000000000000000000000000000000000000000000000000000000000" + // value false
                    "0000000000000000000000000000000000000000000000000000000000000200" + // outer string offset
                    "0000000000000000000000000000000000000000000000000000000000000080" + // uint[] array offset
                    "0000000000000000000000000000000000000000000000000000000000000064" + // value 100
                    "0000000000000000000000000000000000000000000000000000000000000001" + // value true
                    "0000000000000000000000000000000000000000000000000000000000000100" + // string[] array offset
                    "0000000000000000000000000000000000000000000000000000000000000003" + // uint[] array length
                    "00000000000000000000000000000000000000000000000000000000000000c8" + // value 200
                    "000000000000000000000000000000000000000000000000000000000000012c" + // value 300
                    "0000000000000000000000000000000000000000000000000000000000000190" + // value 400
                    "0000000000000000000000000000000000000000000000000000000000000001" + // string array length
                    "0000000000000000000000000000000000000000000000000000000000000020" + // inner string offset
                    "0000000000000000000000000000000000000000000000000000000000000005" + // inner string length
                    "696e6e6572000000000000000000000000000000000000000000000000000000" + // value "inner"
                    "0000000000000000000000000000000000000000000000000000000000000005" + // outer string length
                    "6f75746572000000000000000000000000000000000000000000000000000000" // value "outer"
            )
                .returns(
                    tupleOf(
                        BigInteger.valueOf(500L),
                        tupleOf(
                            listOf(
                                BigInteger.valueOf(200L),
                                BigInteger.valueOf(300L),
                                BigInteger.valueOf(400L)
                            ),
                            tupleOf(
                                BigInteger.valueOf(100L),
                                true
                            ),
                            listOf("inner")
                        ),
                        false,
                        "outer"
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyDecodeComplexNestedTuple() {
        verify("tuple(uint[], tuple(uint[], tuple(uint, bool)[], string[])[], bool, string) is correctly decoded") {
            decoding(
                listOf(
                    TupleType(
                        DynamicArrayType(UintType),
                        DynamicArrayType(
                            TupleType(
                                DynamicArrayType(UintType),
                                DynamicArrayType(
                                    TupleType(
                                        UintType,
                                        BoolType
                                    )
                                ),
                                DynamicArrayType(StringType)
                            )
                        ),
                        BoolType,
                        StringType
                    )
                ),
                "0x" +
                    "0000000000000000000000000000000000000000000000000000000000000020" + // outer tuple offset
                    "0000000000000000000000000000000000000000000000000000000000000080" + // uint[] array offset
                    "0000000000000000000000000000000000000000000000000000000000000140" + // inner tuple offset
                    "0000000000000000000000000000000000000000000000000000000000000000" + // false
                    "00000000000000000000000000000000000000000000000000000000000007c0" + // outer string offset
                    "0000000000000000000000000000000000000000000000000000000000000005" + // uint[] array length
                    "0000000000000000000000000000000000000000000000000000000000000001" + // value 1
                    "0000000000000000000000000000000000000000000000000000000000000002" + // value 2
                    "0000000000000000000000000000000000000000000000000000000000000003" + // value 3
                    "0000000000000000000000000000000000000000000000000000000000000004" + // value 4
                    "0000000000000000000000000000000000000000000000000000000000000005" + // value 5
                    "0000000000000000000000000000000000000000000000000000000000000003" + // tuple array length
                    "0000000000000000000000000000000000000000000000000000000000000060" + // inner tuple1 offset
                    "0000000000000000000000000000000000000000000000000000000000000280" + // inner tuple2 offset
                    "0000000000000000000000000000000000000000000000000000000000000420" + // inner tuple3 offset
                    "0000000000000000000000000000000000000000000000000000000000000060" + // i. tuple1 uint[] offset
                    "00000000000000000000000000000000000000000000000000000000000000a0" + // i. tuple1 tuple[] offset
                    "0000000000000000000000000000000000000000000000000000000000000140" + // i. tuple1 string[] offset
                    "0000000000000000000000000000000000000000000000000000000000000001" + // i. tuple1 uint[] length
                    "000000000000000000000000000000000000000000000000000000000000012c" + // value 300
                    "0000000000000000000000000000000000000000000000000000000000000002" + // i. tuple1 tuple[] length
                    "0000000000000000000000000000000000000000000000000000000000000064" + // value 100
                    "0000000000000000000000000000000000000000000000000000000000000001" + // value true
                    "00000000000000000000000000000000000000000000000000000000000000c8" + // value 200
                    "0000000000000000000000000000000000000000000000000000000000000000" + // value false
                    "0000000000000000000000000000000000000000000000000000000000000002" + // i. tuple1 string[] length
                    "0000000000000000000000000000000000000000000000000000000000000040" + // i. tuple1 string[0] offset
                    "0000000000000000000000000000000000000000000000000000000000000080" + // i. tuple1 string[1] offset
                    "0000000000000000000000000000000000000000000000000000000000000004" + // i. tuple1 string[0] length
                    "7374723100000000000000000000000000000000000000000000000000000000" + // value "str1"
                    "0000000000000000000000000000000000000000000000000000000000000004" + // i. tuple1 string[1] length
                    "7374723200000000000000000000000000000000000000000000000000000000" + // value "str2"
                    "0000000000000000000000000000000000000000000000000000000000000060" + // i. tuple2 uint[] offset
                    "00000000000000000000000000000000000000000000000000000000000000c0" + // i. tuple2 tuple[] offset
                    "0000000000000000000000000000000000000000000000000000000000000120" + // i. tuple2 string[] offset
                    "0000000000000000000000000000000000000000000000000000000000000002" + // i. tuple2 uint[] length
                    "00000000000000000000000000000000000000000000000000000000000007d0" + // value 2000
                    "0000000000000000000000000000000000000000000000000000000000000bb8" + // value 3000
                    "0000000000000000000000000000000000000000000000000000000000000001" + // i. tuple2 tuple[] length
                    "00000000000000000000000000000000000000000000000000000000000003e8" + // value 1000
                    "0000000000000000000000000000000000000000000000000000000000000001" + // value true
                    "0000000000000000000000000000000000000000000000000000000000000001" + // i. tuple2 string[] length
                    "0000000000000000000000000000000000000000000000000000000000000020" + // i. tuple2 string[0] offset
                    "0000000000000000000000000000000000000000000000000000000000000004" + // i. tuple2 string[0] length
                    "7374723300000000000000000000000000000000000000000000000000000000" + // value "str3"
                    "0000000000000000000000000000000000000000000000000000000000000060" + // i. tuple3 uint[] offset
                    "00000000000000000000000000000000000000000000000000000000000000e0" + // i. tuple3 tuple[] offset
                    "00000000000000000000000000000000000000000000000000000000000001c0" + // i. tuple3 string[] offset
                    "0000000000000000000000000000000000000000000000000000000000000003" + // i. tuple3 uint[] length
                    "0000000000000000000000000000000000000000000000000000000000009c40" + // value 40000
                    "000000000000000000000000000000000000000000000000000000000000c350" + // value 50000
                    "0000000000000000000000000000000000000000000000000000000000000000" + // value 0
                    "0000000000000000000000000000000000000000000000000000000000000003" + // i. tuple3 tuple[] length
                    "0000000000000000000000000000000000000000000000000000000000002710" + // value 10000
                    "0000000000000000000000000000000000000000000000000000000000000001" + // value true
                    "0000000000000000000000000000000000000000000000000000000000004e20" + // value 20000
                    "0000000000000000000000000000000000000000000000000000000000000000" + // value false
                    "0000000000000000000000000000000000000000000000000000000000007530" + // value 30000
                    "0000000000000000000000000000000000000000000000000000000000000001" + // value true
                    "0000000000000000000000000000000000000000000000000000000000000001" + // i. tuple3 string[] length
                    "0000000000000000000000000000000000000000000000000000000000000020" + // i. tuple3 string[0] offset
                    "0000000000000000000000000000000000000000000000000000000000000004" + // i. tuple3 string[0] length
                    "7374723400000000000000000000000000000000000000000000000000000000" + // value "str4"
                    "0000000000000000000000000000000000000000000000000000000000000005" + // outer string length
                    "6f75746572000000000000000000000000000000000000000000000000000000" // value "outer"
            )
                .returns(
                    tupleOf(
                        listOf( // uint[]
                            BigInteger.valueOf(1L),
                            BigInteger.valueOf(2L),
                            BigInteger.valueOf(3L),
                            BigInteger.valueOf(4L),
                            BigInteger.valueOf(5L)
                        ),
                        listOf( // tuple(uint[], tuple(uint, bool)[], string[])[]
                            tupleOf( // tuple(uint[], tuple(uint, bool)[], string[])
                                listOf(BigInteger.valueOf(300L)),
                                listOf( // tuple(uint, bool)[]
                                    tupleOf(BigInteger.valueOf(100L), true),
                                    tupleOf(BigInteger.valueOf(200L), false)
                                ),
                                listOf("str1", "str2")
                            ),
                            tupleOf( // tuple(uint[], tuple(uint, bool)[], string[])
                                listOf(BigInteger.valueOf(2000L), BigInteger.valueOf(3000L)),
                                listOf( // tuple(uint, bool)[]
                                    tupleOf(BigInteger.valueOf(1000L), true)
                                ),
                                listOf("str3")
                            ),
                            tupleOf( // tuple(uint[], tuple(uint, bool)[], string[])
                                listOf(BigInteger.valueOf(40000L), BigInteger.valueOf(50000L), BigInteger.ZERO),
                                listOf( // tuple(uint, bool)[]
                                    tupleOf(BigInteger.valueOf(10000L), true),
                                    tupleOf(BigInteger.valueOf(20000L), false),
                                    tupleOf(BigInteger.valueOf(30000L), true)
                                ),
                                listOf("str4")
                            )
                        ),
                        false,
                        "outer"
                    )
                )
        }
    }

    private fun Companion.VerifyMessage.decoding(types: List<AbiType>, encodedInput: String) =
        expectThat(decoder.decode(types, encodedInput))

    private fun ListAssert<Any>.returns(vararg expected: Any) = isEqualTo(expected.toList())

    private fun String.byteList() = toByteArray().toList()

    private fun tupleOf(vararg elems: Any) = Tuple(elems.toList())
}
