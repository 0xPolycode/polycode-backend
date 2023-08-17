package polycode.util

import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Uint
import org.web3j.crypto.Hash
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.DefaultBlockParameterName
import java.math.BigInteger
import java.time.Instant
import java.time.OffsetDateTime
import java.time.Year
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@JvmInline
value class UtcDateTime private constructor(val value: OffsetDateTime) {
    companion object {
        private val ZONE_OFFSET = ZoneOffset.UTC
        operator fun invoke(value: OffsetDateTime) = UtcDateTime(value.withOffsetSameInstant(ZONE_OFFSET))

        fun ofEpochSeconds(value: Long) = UtcDateTime(
            OffsetDateTime.ofInstant(Instant.ofEpochSecond(value), ZONE_OFFSET)
        )

        fun ofInstant(instant: Instant) = UtcDateTime(
            OffsetDateTime.ofInstant(instant, ZONE_OFFSET)
        )
    }

    operator fun plus(duration: Duration): UtcDateTime = UtcDateTime(value + duration.toJavaDuration())
    operator fun minus(duration: Duration): UtcDateTime = UtcDateTime(value - duration.toJavaDuration())

    operator fun plus(duration: DurationSeconds): UtcDateTime = UtcDateTime(value + duration.toJavaDuration())
    operator fun minus(duration: DurationSeconds): UtcDateTime = UtcDateTime(value - duration.toJavaDuration())

    fun atMonthStart(): UtcDateTime =
        UtcDateTime(OffsetDateTime.parse("${value.year}-${value.month.value.toString().padStart(2, '0')}-01T00:00:00Z"))

    fun atMonthEnd(): UtcDateTime {
        val year = value.year
        val isLeapYear = Year.isLeap(year.toLong())
        val lastDay = value.month.length(isLeapYear)
        val monthDigit = value.month.value.toString().padStart(2, '0')
        return UtcDateTime(OffsetDateTime.parse("$year-$monthDigit-${lastDay}T23:59:59.999Z"))
    }

    val iso: String
        get() = DateTimeFormatter.ISO_DATE_TIME.format(value)
}

sealed interface EthereumAddress {
    val value: Address
    val rawValue: String
        get() = value.value

    fun toWalletAddress() = WalletAddress(value)
    fun toContractAddress() = ContractAddress(value)
}

object ZeroAddress : EthereumAddress {
    override val value: Address = Address("0")
}

@JvmInline
value class WalletAddress private constructor(override val value: Address) : EthereumAddress {
    companion object {
        operator fun invoke(value: Address) = WalletAddress(value.toString())
    }

    constructor(value: String) : this(Address(value.lowercase()))
}

@JvmInline
value class ContractAddress private constructor(override val value: Address) : EthereumAddress {
    companion object {
        operator fun invoke(value: Address) = ContractAddress(value.toString())
    }

    constructor(value: String) : this(Address(value.lowercase()))
}

sealed interface EthereumUint {
    val value: Uint
    val rawValue: BigInteger
        get() = value.value
}

@JvmInline
value class Balance(override val value: Uint) : EthereumUint {
    companion object {
        val ZERO = Balance(BigInteger.ZERO)
    }

    constructor(value: BigInteger) : this(Uint(value))
}

@JvmInline
value class DurationSeconds(override val value: Uint) : EthereumUint {
    constructor(value: BigInteger) : this(Uint(value))

    fun toDuration(): Duration = rawValue.longValueExact().seconds
    fun toJavaDuration(): java.time.Duration = toDuration().toJavaDuration()
}

@JvmInline
value class ChainId(val value: Long)

sealed interface BlockParameter {
    fun toWeb3Parameter(): DefaultBlockParameter
}

@JvmInline
value class BlockNumber(val value: BigInteger) : BlockParameter {
    override fun toWeb3Parameter(): DefaultBlockParameter = DefaultBlockParameter.valueOf(value)
}

enum class BlockName(private val web3BlockName: DefaultBlockParameterName) : BlockParameter {
    EARLIEST(DefaultBlockParameterName.EARLIEST),
    LATEST(DefaultBlockParameterName.LATEST),
    PENDING(DefaultBlockParameterName.PENDING);

    override fun toWeb3Parameter() = web3BlockName
}

@JvmInline
value class FunctionData private constructor(val value: String) {
    companion object {
        val EMPTY = FunctionData("0x")
        operator fun invoke(value: String) = FunctionData("0x" + value.removePrefix("0x").lowercase())
    }

    constructor(binary: ByteArray) : this(String(binary))

    val binary: ByteArray // TODO compact conversion
        get() = value.toByteArray()

    val withoutPrefix
        get(): String = value.removePrefix("0x")
}

@JvmInline
value class TransactionHash private constructor(val value: String) {
    companion object {
        operator fun invoke(value: String) = TransactionHash("0x" + value.removePrefix("0x").lowercase())
    }
}

@JvmInline
value class SignedMessage private constructor(val value: String) {
    companion object {
        operator fun invoke(value: String) = SignedMessage(value.lowercase())
    }
}

@JvmInline
value class BaseUrl private constructor(val value: String) {
    companion object {
        operator fun invoke(value: String) = BaseUrl(
            if (value.endsWith('/')) value.dropLast(1) else value
        )
    }
}

@JvmInline
value class ContractId private constructor(val value: String) {
    companion object {
        operator fun invoke(value: String) = ContractId(value.replace('/', '.').lowercase())
    }
}

@JvmInline
value class ContractBinaryData private constructor(val value: String) {
    companion object {
        operator fun invoke(value: String) = ContractBinaryData(value.removePrefix("0x").lowercase())
    }

    constructor(binary: ByteArray) : this(String(binary))

    val binary: ByteArray // TODO compact conversion
        get() = value.toByteArray()

    val withPrefix: String
        get() = "0x$value"
}

@JvmInline
value class ContractTag private constructor(val value: String) {
    companion object {
        operator fun invoke(value: String) = ContractTag(value.replace('/', '.').lowercase())
    }
}

@JvmInline
value class InterfaceId private constructor(val value: String) {
    companion object {
        operator fun invoke(value: String) = InterfaceId(value.replace('/', '.').lowercase())
    }

    fun isImported() = value.contains("imported")
}

@JvmInline
value class EthStorageSlot(val value: BigInteger) {
    companion object {
        private const val HEX_RADIX = 16

        operator fun invoke(value: String) = EthStorageSlot(BigInteger(value.removePrefix("0x"), HEX_RADIX))
    }

    val hex: String
        get() = "0x${value.toString(HEX_RADIX).lowercase().removePrefix("0x")}"
}

@JvmInline
value class Keccak256Hash private constructor(val value: String) {
    companion object {
        operator fun invoke(value: String) = Keccak256Hash(Hash.sha3String(value).lowercase().removePrefix("0x"))
        fun raw(value: String) = Keccak256Hash(value.lowercase().removePrefix("0x"))
    }
}
