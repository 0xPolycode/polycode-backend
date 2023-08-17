package polycode.features.payout.util

import org.web3j.crypto.Hash.sha3 as keccak256
import polycode.generated.jooq.enums.HashFunction as DbHashFunction

enum class HashFunction(
    val toDbEnum: DbHashFunction,
    private val hashFn: (String) -> MerkleHash
) : (String) -> MerkleHash {
    IDENTITY(DbHashFunction.IDENTITY, { MerkleHash(it) }),
    FIXED(DbHashFunction.FIXED, { MerkleHash("0") }),
    KECCAK_256(DbHashFunction.KECCAK_256, { MerkleHash(keccak256(it)) });

    override operator fun invoke(arg: String) = hashFn(arg)

    companion object {
        fun fromDbEnum(value: DbHashFunction): HashFunction {
            return values().find { it.toDbEnum == value }
                ?: throw IllegalStateException("DB enum not added to code: $value")
        }
    }
}
