package polycode.features.payout.util

import java.math.BigInteger

@JvmInline
value class MerkleHash private constructor(val value: String) : Comparable<MerkleHash> {
    companion object {
        private const val HEX_RADIX = 16
        operator fun invoke(value: String) = MerkleHash(value.lowercase())
    }

    operator fun plus(other: MerkleHash): MerkleHash = MerkleHash(value + other.value.replaceFirst("0x", ""))

    override fun compareTo(other: MerkleHash): Int {
        val thisValue = BigInteger(value.replaceFirst("0x", ""), HEX_RADIX)
        val otherValue = BigInteger(other.value.replaceFirst("0x", ""), HEX_RADIX)
        return thisValue.compareTo(otherValue)
    }
}

@JvmInline
value class IpfsHash(val value: String)
