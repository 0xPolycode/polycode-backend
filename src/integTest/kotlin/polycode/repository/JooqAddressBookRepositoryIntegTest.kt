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
import polycode.TestData
import polycode.config.DatabaseConfig
import polycode.exception.AliasAlreadyInUseException
import polycode.features.wallet.addressbook.model.result.AddressBookEntry
import polycode.features.wallet.addressbook.repository.JooqAddressBookRepository
import polycode.generated.jooq.enums.UserIdentifierType
import polycode.generated.jooq.id.AddressBookId
import polycode.generated.jooq.id.UserId
import polycode.generated.jooq.tables.records.AddressBookRecord
import polycode.generated.jooq.tables.records.UserIdentifierRecord
import polycode.testcontainers.SharedTestContainers
import polycode.util.WalletAddress
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@JooqTest
@Import(JooqAddressBookRepository::class, DatabaseConfig::class)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqAddressBookRepositoryIntegTest : TestBase() {

    companion object {
        private const val ALIAS = "ALIAS"
        private val ADDRESS = WalletAddress("a")
        private const val PHONE_NUMBER = "phone-number"
        private const val EMAIL = "email"
        private val OWNER_ID = UserId(UUID.randomUUID())
        private val OWNER_ADDRESS = WalletAddress("cafebabe")
    }

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var repository: JooqAddressBookRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)

        dslContext.executeInsert(
            UserIdentifierRecord(
                id = OWNER_ID,
                userIdentifier = OWNER_ADDRESS.rawValue,
                identifierType = UserIdentifierType.ETH_WALLET_ADDRESS
            )
        )
    }

    @Test
    fun mustCorrectlyFetchAddressBookEntryById() {
        val record = AddressBookRecord(
            id = AddressBookId(UUID.randomUUID()),
            alias = ALIAS,
            walletAddress = ADDRESS,
            phoneNumber = PHONE_NUMBER,
            email = EMAIL,
            createdAt = TestData.TIMESTAMP,
            userId = OWNER_ID
        )

        suppose("some address book entry is stored in database") {
            dslContext.executeInsert(record)
        }

        verify("address book entry is correctly fetched by ID") {
            val result = repository.getById(record.id)

            expectThat(result)
                .isEqualTo(record.toModel())
        }
    }

    @Test
    fun mustCorrectlyFetchAddressBookEntryByAliasAndUserId() {
        val record = AddressBookRecord(
            id = AddressBookId(UUID.randomUUID()),
            alias = ALIAS,
            walletAddress = ADDRESS,
            phoneNumber = PHONE_NUMBER,
            email = EMAIL,
            createdAt = TestData.TIMESTAMP,
            userId = OWNER_ID
        )

        suppose("some address book entry is stored in database") {
            dslContext.executeInsert(record)
        }

        verify("address book entry is correctly fetched by alias and user ID") {
            val result = repository.getByAliasAndUserId(ALIAS, OWNER_ID)

            expectThat(result)
                .isEqualTo(record.toModel())
        }
    }

    @Test
    fun mustCorrectlyFetchAddressBookEntriesByWalletAddress() {
        val records = listOf(
            AddressBookRecord(
                id = AddressBookId(UUID.randomUUID()),
                alias = "alias-1",
                walletAddress = WalletAddress("a"),
                phoneNumber = "phone-number-1",
                email = "email-1",
                createdAt = TestData.TIMESTAMP,
                userId = OWNER_ID
            ),
            AddressBookRecord(
                id = AddressBookId(UUID.randomUUID()),
                alias = "alias-2",
                walletAddress = WalletAddress("b"),
                phoneNumber = "phone-number-2",
                email = "email-2",
                createdAt = TestData.TIMESTAMP + 1.seconds,
                userId = OWNER_ID
            )
        )

        suppose("some address book entries are stored in database") {
            dslContext.batchInsert(records).execute()
        }

        verify("address book entries are correctly fetched by wallet address") {
            val result = repository.getAllByWalletAddress(OWNER_ADDRESS)

            expectThat(result)
                .isEqualTo(records.map { it.toModel() })
        }
    }

    @Test
    fun mustCorrectlyStoreAddressBookEntry() {
        val addressBookEntry = AddressBookEntry(
            id = AddressBookId(UUID.randomUUID()),
            alias = ALIAS,
            address = ADDRESS,
            phoneNumber = PHONE_NUMBER,
            email = EMAIL,
            createdAt = TestData.TIMESTAMP,
            userId = OWNER_ID
        )

        val storedAddressBookEntry = suppose("address book entry is stored in database") {
            repository.store(addressBookEntry)
        }

        verify("storing address book entry returns correct result") {
            expectThat(storedAddressBookEntry)
                .isEqualTo(addressBookEntry)
        }

        verify("address book entry was stored in database") {
            val result = repository.getById(addressBookEntry.id)

            expectThat(result)
                .isEqualTo(addressBookEntry)
        }

        verify("storing address book entry with conflicting alias throws AliasAlreadyInUseException") {
            expectThrows<AliasAlreadyInUseException> {
                repository.store(addressBookEntry.copy(id = AddressBookId(UUID.randomUUID())))
            }
        }
    }

    @Test
    fun mustCorrectlyUpdateAddressBookEntry() {
        val addressBookEntry = AddressBookEntry(
            id = AddressBookId(UUID.randomUUID()),
            alias = ALIAS,
            address = ADDRESS,
            phoneNumber = PHONE_NUMBER,
            email = EMAIL,
            createdAt = TestData.TIMESTAMP,
            userId = OWNER_ID
        )

        suppose("address book entry is stored in database") {
            repository.store(addressBookEntry)
        }

        val nonNullUpdates = AddressBookEntry(
            id = addressBookEntry.id,
            alias = "new-alias",
            address = WalletAddress("cafe0babe1"),
            phoneNumber = "new-phone-number",
            email = "new-email",
            createdAt = TestData.TIMESTAMP + 1.seconds,
            userId = OWNER_ID
        )

        val updatedNonNullAddressBookEntry = suppose("address book entry is updated in database") {
            repository.update(nonNullUpdates)
        }

        verify("updating address book entry returns correct result") {
            expectThat(updatedNonNullAddressBookEntry)
                .isEqualTo(nonNullUpdates.copy(createdAt = addressBookEntry.createdAt))
        }

        verify("address book entry was updated in database") {
            val result = repository.getById(addressBookEntry.id)

            expectThat(result)
                .isEqualTo(nonNullUpdates.copy(createdAt = addressBookEntry.createdAt))
        }

        val nullUpdates = nonNullUpdates.copy(
            phoneNumber = null,
            email = null,
            createdAt = TestData.TIMESTAMP + 1.seconds
        )

        val updatedNullAddressBookEntry = suppose("address book entry is updated in database with null values") {
            repository.update(nullUpdates)
        }

        verify("updating address book entry with null values returns correct result") {
            expectThat(updatedNullAddressBookEntry)
                .isEqualTo(nullUpdates.copy(createdAt = addressBookEntry.createdAt))
        }

        verify("address book entry was updated in database with null values") {
            val result = repository.getById(addressBookEntry.id)

            expectThat(result)
                .isEqualTo(nullUpdates.copy(createdAt = addressBookEntry.createdAt))
        }

        val otherEntry = AddressBookEntry(
            id = AddressBookId(UUID.randomUUID()),
            alias = "other-alias",
            address = WalletAddress("c"),
            phoneNumber = "other-phone-number",
            email = "other-email",
            createdAt = TestData.TIMESTAMP,
            userId = OWNER_ID
        )

        suppose("other address book entry is stored in database") {
            repository.store(otherEntry)
        }

        verify("updating entry to have same alias as other entry throws AliasAlreadyInUseException") {
            expectThrows<AliasAlreadyInUseException> {
                repository.update(otherEntry.copy(alias = "new-alias"))
            }
        }
    }

    private fun AddressBookRecord.toModel() =
        AddressBookEntry(
            id = id,
            alias = alias,
            address = walletAddress,
            phoneNumber = phoneNumber,
            email = email,
            createdAt = createdAt,
            userId = userId
        )
}
