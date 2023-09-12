@file:Suppress("MagicNumber")

package polycode.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Configuration
import polycode.util.ChainId
import polycode.util.WalletAddress
import java.math.BigInteger
import java.nio.file.Path
import java.security.interfaces.RSAPrivateCrtKey
import java.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Configuration
@ConfigurationPropertiesScan
@ConfigurationProperties(prefix = "polycode")
class ApplicationProperties {
    var chain: Map<ChainId, ChainProperties> = emptyMap()
    var infuraId: String = ""
}

@ConstructorBinding
@ConfigurationProperties(prefix = "polycode.jwt")
data class JwtProperties(
    val privateKey: RSAPrivateCrtKey,
    val tokenValidity: Duration,
    val walletLoginRequestValidity: Duration
)

@ConstructorBinding
@ConfigurationProperties(prefix = "polycode.ipfs")
data class IpfsProperties(
    val url: String = "https://api.pinata.cloud/",
    val apiKey: String = "",
    val secretApiKey: String = ""
)

@ConstructorBinding
data class ChainProperties(
    val name: String,
    val rpcUrl: String,
    val infuraUrl: String?,
    val startBlockNumber: BigInteger?,
    val minBlockConfirmationsForCaching: BigInteger?,
    val chainExplorerApiUrl: String?,
    val chainExplorerApiKey: String?,
    val latestBlockCacheDuration: Duration = 5.seconds.toJavaDuration()
)

@ConstructorBinding
@ConfigurationProperties(prefix = "polycode.create-payout-queue")
data class PayoutQueueProperties(
    val polling: Long = 5_000L,
    val initialDelay: Long = 15_000L
)

@ConstructorBinding
@ConfigurationProperties(prefix = "polycode.contract-decorators")
data class ContractDecoratorProperties(
    val contractsDirectory: Path?,
    val interfacesDirectory: Path?,
    val ignoredDirs: List<String> = listOf(".git"),
    val fillChangePollInterval: Duration = 1.minutes.toJavaDuration(),
    val fileChangeQuietInterval: Duration = 30.seconds.toJavaDuration()
)

@ConstructorBinding
@ConfigurationProperties(prefix = "polycode.meta-pixel-properties")
data class MetaPixelProperties(
    val accessToken: String?,
    val pixelId: String?
)

@ConstructorBinding
@ConfigurationProperties(prefix = "polycode.contract-manifest-service")
data class ContractManifestServiceProperties(
    val baseUrl: String?,
    val decompileContractPath: String = "/decompile-contract",
    val functionSignaturePath: String = "/function-signature/{signature}"
)

@ConstructorBinding
@ConfigurationProperties(prefix = "polycode.api-rate")
data class ApiRateProperties(
    val usagePeriodDuration: Duration = 30.days.toJavaDuration(),
    val freeTierWriteRequests: Long = 200L,
    val freeTierReadRequests: Long = 500_000L
)

@ConstructorBinding
@ConfigurationProperties(prefix = "polycode.admin")
data class AdminProperties(
    val wallets: Set<WalletAddress> = emptySet()
)

@ConstructorBinding
@ConfigurationProperties(prefix = "polycode.blacklist-api")
data class BlacklistApiProperties(
    val url: String = "https://app.hoptrail.io/api/eth/check/",
    val timeout: Duration = 1.seconds.toJavaDuration()
)
