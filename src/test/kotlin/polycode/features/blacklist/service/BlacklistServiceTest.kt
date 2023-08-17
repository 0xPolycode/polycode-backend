package polycode.features.blacklist.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import polycode.TestBase
import polycode.config.AdminProperties
import polycode.exception.AccessForbiddenException
import polycode.features.api.access.model.result.UserWalletAddressIdentifier
import polycode.features.blacklist.repository.BlacklistedAddressRepository
import polycode.generated.jooq.id.UserId
import polycode.util.WalletAddress
import java.util.UUID

class BlacklistServiceTest : TestBase() {

    companion object {
        private val USER_IDENTIFIER = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            walletAddress = WalletAddress("abc")
        )
        private val ADDRESS = WalletAddress("cafebabe")
    }

    @Test
    fun mustCorrectlyAddAddressToBlacklistWhenUserIsAdmin() {
        val adminProperties = suppose("user is admin") {
            AdminProperties(wallets = setOf(USER_IDENTIFIER.walletAddress))
        }

        val blacklistedAddressRepository = mock<BlacklistedAddressRepository>()
        val service = BlacklistServiceImpl(
            blacklistedAddressRepository = blacklistedAddressRepository,
            adminProperties = adminProperties
        )

        verify("address is correctly added to blacklist") {
            service.addAddress(USER_IDENTIFIER, ADDRESS)

            expectInteractions(blacklistedAddressRepository) {
                once.addAddress(ADDRESS)
            }
        }
    }

    @Test
    fun mustThrowAccessForbiddenExceptionWhenNonAdminUserAddsAddressToBlacklist() {
        val adminProperties = suppose("user is not admin") {
            AdminProperties()
        }

        val blacklistedAddressRepository = mock<BlacklistedAddressRepository>()
        val service = BlacklistServiceImpl(
            blacklistedAddressRepository = blacklistedAddressRepository,
            adminProperties = adminProperties
        )

        verify("AccessForbiddenException is thrown") {
            expectThrows<AccessForbiddenException> {
                service.addAddress(USER_IDENTIFIER, ADDRESS)
            }
        }
    }

    @Test
    fun mustCorrectlyRemoveAddressFromBlacklistWhenUserIsAdmin() {
        val adminProperties = suppose("user is admin") {
            AdminProperties(wallets = setOf(USER_IDENTIFIER.walletAddress))
        }

        val blacklistedAddressRepository = mock<BlacklistedAddressRepository>()
        val service = BlacklistServiceImpl(
            blacklistedAddressRepository = blacklistedAddressRepository,
            adminProperties = adminProperties
        )

        verify("address is correctly removed from blacklist") {
            service.removeAddress(USER_IDENTIFIER, ADDRESS)

            expectInteractions(blacklistedAddressRepository) {
                once.removeAddress(ADDRESS)
            }
        }
    }

    @Test
    fun mustThrowAccessForbiddenExceptionWhenNonAdminUserRemovesAddressFromBlacklist() {
        val adminProperties = suppose("user is not admin") {
            AdminProperties()
        }

        val blacklistedAddressRepository = mock<BlacklistedAddressRepository>()
        val service = BlacklistServiceImpl(
            blacklistedAddressRepository = blacklistedAddressRepository,
            adminProperties = adminProperties
        )

        verify("AccessForbiddenException is thrown") {
            expectThrows<AccessForbiddenException> {
                service.removeAddress(USER_IDENTIFIER, ADDRESS)
            }
        }
    }

    @Test
    fun mustCorrectlyListBlacklistAddressesWhenUserIsAdmin() {
        val adminProperties = suppose("user is admin") {
            AdminProperties(wallets = setOf(USER_IDENTIFIER.walletAddress))
        }

        val blacklistedAddressRepository = mock<BlacklistedAddressRepository>()

        suppose("list of blacklisted addresses will be returned") {
            call(blacklistedAddressRepository.listAddresses())
                .willReturn(listOf(ADDRESS))
        }

        val service = BlacklistServiceImpl(
            blacklistedAddressRepository = blacklistedAddressRepository,
            adminProperties = adminProperties
        )

        verify("correct list of blacklisted addresses is returned") {
            expectThat(service.listAddresses(USER_IDENTIFIER))
                .isEqualTo(listOf(ADDRESS))
        }
    }

    @Test
    fun mustThrowAccessForbiddenExceptionWhenNonAdminUserListsBlacklistedAddresses() {
        val adminProperties = suppose("user is not admin") {
            AdminProperties()
        }

        val blacklistedAddressRepository = mock<BlacklistedAddressRepository>()
        val service = BlacklistServiceImpl(
            blacklistedAddressRepository = blacklistedAddressRepository,
            adminProperties = adminProperties
        )

        verify("AccessForbiddenException is thrown") {
            expectThrows<AccessForbiddenException> {
                service.listAddresses(USER_IDENTIFIER)
            }
        }
    }
}
