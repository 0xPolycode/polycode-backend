@file:Suppress("FunctionName", "TooManyFunctions")

package polycode.util

import org.jooq.JSON
import polycode.config.JsonConfig
import polycode.features.contract.deployment.model.json.ArtifactJson
import polycode.features.contract.deployment.model.json.ManifestJson
import polycode.features.payout.util.AssetSnapshotFailureCause
import polycode.features.payout.util.AssetSnapshotStatus
import polycode.features.payout.util.HashFunction
import polycode.features.payout.util.IpfsHash
import polycode.features.payout.util.MerkleHash
import polycode.generated.jooq.converters.converter
import java.math.BigInteger
import java.time.OffsetDateTime
import polycode.generated.jooq.enums.AssetSnapshotFailureCause as DbAssetSnapshotFailureCause
import polycode.generated.jooq.enums.AssetSnapshotStatus as DbAssetSnapshotStatus
import polycode.generated.jooq.enums.HashFunction as DbHashFunction

private val objectMapper = JsonConfig().objectMapper()

fun ChainIdConverter() = converter({ it: Long -> ChainId(it) }, { it.value })

fun ContractAddressConverter() = converter({ it: String -> ContractAddress(it) }, { it.rawValue })

fun WalletAddressConverter() = converter({ it: String -> WalletAddress(it) }, { it.rawValue })

fun BalanceConverter() = converter({ it: BigInteger -> Balance(it) }, { it.rawValue })

fun BlockNumberConverter() = converter({ it: BigInteger -> BlockNumber(it) }, { it.value })

fun TransactionHashConverter() = converter({ it: String -> TransactionHash(it) }, { it.value })

fun SignedMessageConverter() = converter({ it: String -> SignedMessage(it) }, { it.value })

fun DurationSecondsConverter() = converter({ it: BigInteger -> DurationSeconds(it) }, { it.rawValue })

fun UtcDateTimeConverter() = converter({ it: OffsetDateTime -> UtcDateTime(it) }, { it.value })

fun BaseUrlConverter() = converter({ it: String -> BaseUrl(it) }, { it.value })

fun ContractIdConverter() = converter({ it: String -> ContractId(it) }, { it.value })

fun ContractBinaryDataConverter() = converter({ it: ByteArray -> ContractBinaryData(it) }, { it.binary })

fun FunctionDataConverter() = converter({ it: ByteArray -> FunctionData(it) }, { it.binary })

fun JsonNodeConverter() = converter(
    { it: JSON -> objectMapper.readTree(it.data()) },
    { JSON.valueOf(objectMapper.writeValueAsString(it)) }
)

fun ManifestJsonConverter() = converter(
    { it: JSON -> objectMapper.readValue(it.data(), ManifestJson::class.java) },
    { JSON.valueOf(objectMapper.writeValueAsString(it)) }
)

fun ArtifactJsonConverter() = converter(
    { it: JSON -> objectMapper.readValue(it.data(), ArtifactJson::class.java) },
    { JSON.valueOf(objectMapper.writeValueAsString(it)) }
)

fun MerkleHashConverter() = converter({ it: String -> MerkleHash(it) }, { it.value })

fun IpfsHashConverter() = converter({ it: String -> IpfsHash(it) }, { it.value })

fun HashFunctionConverter() = converter({ it: DbHashFunction -> HashFunction.fromDbEnum(it) }, { it.toDbEnum })

fun AssetSnapshotStatusConverter() = converter(
    { it: DbAssetSnapshotStatus -> AssetSnapshotStatus.fromDbEnum(it) },
    { it.toDbEnum }
)

fun AssetSnapshotFailureCauseConverter() = converter(
    { it: DbAssetSnapshotFailureCause -> AssetSnapshotFailureCause.fromDbEnum(it) },
    { it.toDbEnum }
)
