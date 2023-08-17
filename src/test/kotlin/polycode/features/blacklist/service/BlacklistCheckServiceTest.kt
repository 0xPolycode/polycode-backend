package polycode.features.blacklist.service

import org.junit.jupiter.api.Test
import org.mockito.internal.stubbing.answers.AnswersWithDelay
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import polycode.TestBase
import polycode.config.BlacklistApiProperties
import polycode.features.blacklist.repository.BlacklistedAddressRepository
import polycode.features.blacklist.service.BlacklistCheckServiceImpl.Companion.SuspiciousActivityResponse
import polycode.util.WalletAddress
import polycode.util.ZeroAddress
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

class BlacklistCheckServiceTest : TestBase() {

    companion object {
        private val ADDRESS = WalletAddress("cafebabe")
    }

    @Test
    fun mustOnlyCallRepositoryWhenBlacklistedAddressExistsInDatabase() {
        val blacklistedAddressRepository = mock<BlacklistedAddressRepository>()

        suppose("blacklisted address is found in the repository") {
            call(blacklistedAddressRepository.exists(ADDRESS))
                .willReturn(true)
        }

        val basicJsonRestTemplate = mock<RestTemplate>()

        val service = BlacklistCheckServiceImpl(
            blacklistedAddressRepository = blacklistedAddressRepository,
            basicJsonRestTemplate = basicJsonRestTemplate,
            blacklistApiProperties = BlacklistApiProperties()
        )

        verify("only repository call is made") {
            expectThat(service.exists(ADDRESS))
                .isTrue()

            expectInteractions(blacklistedAddressRepository) {
                once.exists(ADDRESS)
            }

            expectNoInteractions(basicJsonRestTemplate)
        }
    }

    @Test
    fun mustWaitForApiCallToFinishAndReturnResponseWhenBlacklistedAddressDoesNotExistInDatabase() {
        val blacklistedAddressRepository = mock<BlacklistedAddressRepository>()

        suppose("blacklisted address is not found in the repository") {
            call(blacklistedAddressRepository.exists(ADDRESS))
                .willReturn(false)
        }

        val basicJsonRestTemplate = mock<RestTemplate>()
        val blacklistApiProperties = BlacklistApiProperties()

        suppose("API will return some suspicious address") {
            call(
                basicJsonRestTemplate.getForEntity(
                    "${blacklistApiProperties.url.removeSuffix("/")}/${ADDRESS.rawValue}",
                    Array<SuspiciousActivityResponse>::class.java
                )
            )
                .willReturn(
                    ResponseEntity.ok(
                        arrayOf(SuspiciousActivityResponse(WalletAddress("dead").rawValue))
                    )
                )
        }

        val service = BlacklistCheckServiceImpl(
            blacklistedAddressRepository = blacklistedAddressRepository,
            basicJsonRestTemplate = basicJsonRestTemplate,
            blacklistApiProperties = BlacklistApiProperties()
        )

        verify("true is returned and new address is stored to the database") {
            expectThat(service.exists(ADDRESS))
                .isTrue()

            expectInteractions(blacklistedAddressRepository) {
                once.exists(ADDRESS)
                once.addAddress(ADDRESS)
            }

            expectInteractions(basicJsonRestTemplate) {
                once.getForEntity(
                    "${blacklistApiProperties.url.removeSuffix("/")}/${ADDRESS.rawValue}",
                    Array<SuspiciousActivityResponse>::class.java
                )
            }
        }
    }

    @Test
    fun mustNotStoreAddressWithZeroAddressInteractionInDatabase() {
        val blacklistedAddressRepository = mock<BlacklistedAddressRepository>()

        suppose("blacklisted address is not found in the repository") {
            call(blacklistedAddressRepository.exists(ADDRESS))
                .willReturn(false)
        }

        val basicJsonRestTemplate = mock<RestTemplate>()
        val blacklistApiProperties = BlacklistApiProperties()

        suppose("API will return some non-suspicious address") {
            call(
                basicJsonRestTemplate.getForEntity(
                    "${blacklistApiProperties.url.removeSuffix("/")}/${ADDRESS.rawValue}",
                    Array<SuspiciousActivityResponse>::class.java
                )
            )
                .willReturn(
                    ResponseEntity.ok(
                        arrayOf(SuspiciousActivityResponse(ZeroAddress.rawValue))
                    )
                )
        }

        val service = BlacklistCheckServiceImpl(
            blacklistedAddressRepository = blacklistedAddressRepository,
            basicJsonRestTemplate = basicJsonRestTemplate,
            blacklistApiProperties = BlacklistApiProperties()
        )

        verify("false is returned and no address is stored to the database") {
            expectThat(service.exists(ADDRESS))
                .isFalse()

            expectInteractions(blacklistedAddressRepository) {
                once.exists(ADDRESS)
            }

            expectInteractions(basicJsonRestTemplate) {
                once.getForEntity(
                    "${blacklistApiProperties.url.removeSuffix("/")}/${ADDRESS.rawValue}",
                    Array<SuspiciousActivityResponse>::class.java
                )
            }
        }
    }

    @Test
    fun mustNotStoreAddressWhenRestTemplateThrowsException() {
        val blacklistedAddressRepository = mock<BlacklistedAddressRepository>()

        suppose("blacklisted address is not found in the repository") {
            call(blacklistedAddressRepository.exists(ADDRESS))
                .willReturn(false)
        }

        val basicJsonRestTemplate = mock<RestTemplate>()
        val blacklistApiProperties = BlacklistApiProperties()

        suppose("API will return some non-suspicious address") {
            call(
                basicJsonRestTemplate.getForEntity(
                    "${blacklistApiProperties.url.removeSuffix("/")}/${ADDRESS.rawValue}",
                    Array<SuspiciousActivityResponse>::class.java
                )
            )
                .willThrow(RestClientException::class.java)
        }

        val service = BlacklistCheckServiceImpl(
            blacklistedAddressRepository = blacklistedAddressRepository,
            basicJsonRestTemplate = basicJsonRestTemplate,
            blacklistApiProperties = BlacklistApiProperties()
        )

        verify("false is returned and no address is stored to the database") {
            expectThat(service.exists(ADDRESS))
                .isFalse()

            expectInteractions(blacklistedAddressRepository) {
                once.exists(ADDRESS)
            }

            expectInteractions(basicJsonRestTemplate) {
                once.getForEntity(
                    "${blacklistApiProperties.url.removeSuffix("/")}/${ADDRESS.rawValue}",
                    Array<SuspiciousActivityResponse>::class.java
                )
            }
        }
    }

    @Test
    fun mustReturnFalseButStillInsertSuspiciousAddressInDatabaseWhenCallTimesOut() {
        val blacklistedAddressRepository = mock<BlacklistedAddressRepository>()

        suppose("blacklisted address is not found in the repository") {
            call(blacklistedAddressRepository.exists(ADDRESS))
                .willReturn(false)
        }

        val basicJsonRestTemplate = mock<RestTemplate>()
        val blacklistApiProperties = BlacklistApiProperties()

        suppose("API will return some suspicious address") {
            call(
                basicJsonRestTemplate.getForEntity(
                    "${blacklistApiProperties.url.removeSuffix("/")}/${ADDRESS.rawValue}",
                    Array<SuspiciousActivityResponse>::class.java
                )
            )
                .willAnswer(
                    AnswersWithDelay(100L) {
                        ResponseEntity.ok(
                            arrayOf(SuspiciousActivityResponse(WalletAddress("dead").rawValue))
                        )
                    }
                )
        }

        val service = BlacklistCheckServiceImpl(
            blacklistedAddressRepository = blacklistedAddressRepository,
            basicJsonRestTemplate = basicJsonRestTemplate,
            blacklistApiProperties = BlacklistApiProperties(timeout = 1.milliseconds.toJavaDuration())
        )

        verify("true is returned and new address is stored to the database") {
            expectThat(service.exists(ADDRESS))
                .isFalse()

            Thread.sleep(500L) // wait for future to finish

            expectInteractions(blacklistedAddressRepository) {
                once.exists(ADDRESS)
                once.addAddress(ADDRESS)
            }

            expectInteractions(basicJsonRestTemplate) {
                once.getForEntity(
                    "${blacklistApiProperties.url.removeSuffix("/")}/${ADDRESS.rawValue}",
                    Array<SuspiciousActivityResponse>::class.java
                )
            }
        }
    }
}
