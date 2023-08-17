package polycode.repository

import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import polycode.TestBase
import polycode.config.DatabaseConfig
import polycode.features.api.access.model.result.UserWalletAddressIdentifier
import polycode.features.api.access.repository.JooqUserIdentifierRepository
import polycode.generated.jooq.enums.UserIdentifierType
import polycode.generated.jooq.id.UserId
import polycode.generated.jooq.tables.records.UserIdentifierRecord
import polycode.testcontainers.SharedTestContainers
import polycode.util.WalletAddress
import java.util.UUID

@JooqTest
@Import(JooqUserIdentifierRepository::class, DatabaseConfig::class)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqUserIdentifierRepositoryIntegTest : TestBase() {

    companion object {
        private val USER_WALLET_ADDRESS = WalletAddress("fade")
        private val IDENTIFIER_TYPE = UserIdentifierType.ETH_WALLET_ADDRESS
    }

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var repository: JooqUserIdentifierRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)
    }

    @Test
    fun mustCorrectlyFetchUserIdentifierById() {
        val id = UserId(UUID.randomUUID())

        suppose("some user identifier is stored in database") {
            dslContext.executeInsert(
                UserIdentifierRecord(
                    id = id,
                    userIdentifier = USER_WALLET_ADDRESS.rawValue,
                    identifierType = IDENTIFIER_TYPE
                )
            )
        }

        verify("user identifier is correctly fetched by ID") {
            val result = repository.getById(id)

            expectThat(result)
                .isEqualTo(
                    UserWalletAddressIdentifier(
                        id = id,
                        walletAddress = USER_WALLET_ADDRESS
                    )
                )
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentUserIdentifierById() {
        verify("null is returned when fetching non-existent user identifier") {
            val result = repository.getById(UserId(UUID.randomUUID()))

            expectThat(result)
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyFetchUserIdentifierByIdentifier() {
        val id = UserId(UUID.randomUUID())

        suppose("some user identifier is stored in database") {
            dslContext.executeInsert(
                UserIdentifierRecord(
                    id = id,
                    userIdentifier = USER_WALLET_ADDRESS.rawValue,
                    identifierType = IDENTIFIER_TYPE
                )
            )
        }

        verify("user identifier is correctly fetched by identifier") {
            val result = repository.getByUserIdentifier(USER_WALLET_ADDRESS.rawValue, IDENTIFIER_TYPE)

            expectThat(result)
                .isEqualTo(
                    UserWalletAddressIdentifier(
                        id = id,
                        walletAddress = USER_WALLET_ADDRESS
                    )
                )
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentUserIdentifierByIdentifier() {
        verify("null is returned when fetching non-existent user identifier") {
            val result = repository.getByUserIdentifier("???", IDENTIFIER_TYPE)

            expectThat(result)
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyFetchUserIdentifierByWalletAddress() {
        val id = UserId(UUID.randomUUID())
        val walletAddress = WalletAddress("0cafe0babe")

        suppose("some user identifier is stored in database") {
            dslContext.executeInsert(
                UserIdentifierRecord(
                    id = id,
                    userIdentifier = walletAddress.rawValue,
                    identifierType = IDENTIFIER_TYPE
                )
            )
        }

        verify("user identifier is correctly fetched by identifier") {
            val result = repository.getByWalletAddress(walletAddress)

            expectThat(result)
                .isEqualTo(
                    UserWalletAddressIdentifier(
                        id = id,
                        walletAddress = walletAddress
                    )
                )
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentUserIdentifierByWalletAddress() {
        verify("null is returned when fetching non-existent user identifier") {
            val result = repository.getByWalletAddress(WalletAddress("dead"))

            expectThat(result)
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyStoreUserIdentifier() {
        val id = UserId(UUID.randomUUID())
        val userIdentifier = UserWalletAddressIdentifier(
            id = id,
            walletAddress = USER_WALLET_ADDRESS
        )

        val storedUserIdentifier = suppose("user identifier is stored in database") {
            repository.store(userIdentifier)
        }

        verify("storing user identifier returns correct result") {
            expectThat(storedUserIdentifier)
                .isEqualTo(userIdentifier)
        }

        verify("user identifier was stored in database") {
            val result = repository.getById(id)

            expectThat(result)
                .isEqualTo(userIdentifier)
        }
    }
}
