package polycode.features.blacklist.service

import mu.KLogging
import org.springframework.beans.factory.DisposableBean
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import polycode.config.BlacklistApiProperties
import polycode.features.blacklist.repository.BlacklistedAddressRepository
import polycode.util.EthereumAddress
import polycode.util.WalletAddress
import polycode.util.ZeroAddress
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Service
class BlacklistCheckServiceImpl(
    private val blacklistedAddressRepository: BlacklistedAddressRepository,
    private val basicJsonRestTemplate: RestTemplate,
    private val blacklistApiProperties: BlacklistApiProperties
) : BlacklistCheckService, DisposableBean {

    companion object : KLogging() {
        internal data class SuspiciousActivityResponse(
            val address: String?
        )
    }

    private val executorService = Executors.newCachedThreadPool()

    override fun destroy() {
        logger.info { "Shutting down blacklist checker executor service..." }
        executorService.shutdown()
    }

    override fun exists(address: EthereumAddress): Boolean =
        blacklistedAddressRepository.exists(address) || waitForApiCheck(address)

    private fun waitForApiCheck(address: EthereumAddress): Boolean =
        try {
            apiCheckAddress(address).get(blacklistApiProperties.timeout.toMillis(), TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            logger.info { "Timed out API check address for suspicious activities: $address" }
            false
        }

    private fun apiCheckAddress(address: EthereumAddress): Future<Boolean> =
        executorService.submit(
            Callable {
                logger.info { "API checking address for suspicious activities: $address" }

                val suspiciousActivities = try {
                    basicJsonRestTemplate.getForEntity(
                        "${blacklistApiProperties.url.removeSuffix("/")}/${address.rawValue}",
                        Array<SuspiciousActivityResponse>::class.java
                    ).body?.toList().orEmpty()
                } catch (e: RestClientException) {
                    logger.warn(e) { "Failed to API check address for suspicious activities: $address" }
                    emptyList()
                }

                val isSuspicious = suspiciousActivities.mapNotNull { it.address }
                    .any { WalletAddress(it) != ZeroAddress.toWalletAddress() }

                if (isSuspicious) {
                    blacklistedAddressRepository.addAddress(address)
                }

                isSuspicious
            }
        )
}
