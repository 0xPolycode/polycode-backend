package polycode.features.contract.abi.service

import org.springframework.stereotype.Service
import polycode.exception.AbiDecodingException
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
import polycode.util.WalletAddress
import java.math.BigInteger
import kotlin.math.ceil

@Service
class EthereumAbiDecoderService : AbiDecoderService {

    companion object {
        private const val BYTE_LENGTH = 2
        private const val BYTES_PER_VALUE = 32
        private const val VALUE_LENGTH = BYTES_PER_VALUE * BYTE_LENGTH
        private const val HEX_RADIX = 16
        private val MAX_UINT_256 = BigInteger("f".repeat(VALUE_LENGTH), HEX_RADIX)
    }

    @Suppress("TooGenericExceptionCaught")
    override fun decode(types: List<AbiType>, encodedInput: String): List<Any> {
        try {
            return unsafeDecode(types, encodedInput)
        } catch (e: RuntimeException) {
            throw AbiDecodingException(types, e)
        }
    }

    private fun unsafeDecode(types: List<AbiType>, encodedInput: String): List<Any> {
        val withoutPrefix = encodedInput.removePrefix("0x")
        var index = 0

        return types.map { type ->
            val (value, indexIncrement) = if (type.isDynamic()) {
                val offset = withoutPrefix.takeValue(index).parseLength() * BYTE_LENGTH
                parseType(type, withoutPrefix.substring(offset)).first.withIndexIncrement(type)
            } else {
                parseType(type, withoutPrefix.substring(index * VALUE_LENGTH))
            }

            index += indexIncrement
            value
        }
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    private fun parseType(type: AbiType, value: String): Pair<Any, Int> =
        when (type) {
            UintType -> value.takeValue().parseHexBigInteger()
            IntType -> value.takeValue().let { it.parseHexBigInteger().handleTwosComplement(it) }
            AddressType -> WalletAddress(value.takeValue()).rawValue
            BoolType -> (BigInteger(value.takeValue(), HEX_RADIX) == BigInteger.ONE)
            is StaticBytesType -> value.takeValue().parseHexBytes(type.size)
            is StaticArrayType<*> -> decode(List(type.size) { type.elem }, value)

            is DynamicArrayType<*> -> {
                val length = value.takeValue().parseLength()
                decode(List(length) { type.elem }, value.substring(VALUE_LENGTH))
            }

            DynamicBytesType ->
                value.takeValue().parseLength().let { length ->
                    value.takeValue(index = 1, n = length.toValueLength()).parseHexBytes(length)
                }

            StringType -> {
                value.takeValue().parseLength().let { length ->
                    val stringBytes = value.takeValue(index = 1, n = length.toValueLength()).parseHexBytes(length)
                    String(stringBytes.toByteArray())
                }
            }

            is TupleType -> Tuple(decode(type.elems, value))
        }.withIndexIncrement(type)

    private fun Any.withIndexIncrement(type: AbiType): Pair<Any, Int> = Pair(this, type.valueSize())

    private fun String.takeValue(index: Int = 0, n: Int = 1) =
        substring(index * VALUE_LENGTH, (index + n) * VALUE_LENGTH)

    private fun String.parseLength() = parseHexBigInteger().intValueExact()

    private fun Int.toValueLength() = ceil(this.toDouble() / BYTES_PER_VALUE).toInt()

    private fun BigInteger.handleTwosComplement(value: String) =
        if (value.startsWith("f")) (MAX_UINT_256 - this + BigInteger.ONE).negate() else this

    private fun String.parseHexBigInteger() = BigInteger(this, HEX_RADIX)

    private fun String.parseHexBytes(size: Int) =
        take(size * BYTE_LENGTH).chunked(BYTE_LENGTH).map { it.toUByte(HEX_RADIX).toByte() }.toList()
}
