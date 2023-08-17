package polycode.controller

import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import polycode.ControllerTestBase
import polycode.TestData
import polycode.exception.ErrorCode
import polycode.features.wallet.addressbook.model.response.AddressBookEntriesResponse
import polycode.features.wallet.addressbook.model.response.AddressBookEntryResponse
import polycode.features.wallet.addressbook.model.result.AddressBookEntry
import polycode.features.wallet.addressbook.repository.AddressBookRepository
import polycode.generated.jooq.enums.UserIdentifierType
import polycode.generated.jooq.id.AddressBookId
import polycode.generated.jooq.id.UserId
import polycode.generated.jooq.tables.records.UserIdentifierRecord
import polycode.security.WithMockUser
import polycode.util.WalletAddress
import java.util.UUID

class AddressBookControllerApiTest : ControllerTestBase() {

    companion object {
        private val OWNER_ID = UserId(UUID.randomUUID())
        private const val OWNER_ADDRESS = "abc123"
        private val OTHER_OWNER_ID = UserId(UUID.randomUUID())
        private const val OTHER_OWNER_ADDRESS = "def456"
    }

    @Autowired
    private lateinit var addressBookRepository: AddressBookRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)

        dslContext.executeInsert(
            UserIdentifierRecord(
                id = OWNER_ID,
                userIdentifier = WalletAddress(OWNER_ADDRESS).rawValue,
                identifierType = UserIdentifierType.ETH_WALLET_ADDRESS
            )
        )

        dslContext.executeInsert(
            UserIdentifierRecord(
                id = OTHER_OWNER_ID,
                userIdentifier = WalletAddress(OTHER_OWNER_ADDRESS).rawValue,
                identifierType = UserIdentifierType.ETH_WALLET_ADDRESS
            )
        )
    }

    @Test
    @WithMockUser(OWNER_ADDRESS)
    fun mustCorrectlyCreateAddressBookEntry() {
        val alias = "alias"
        val address = WalletAddress("abc")
        val phoneNumber = "phone-number"
        val email = "email"

        val response = suppose("request to create address book entry is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/address-book")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "$alias",
                                "address": "${address.rawValue}",
                                "phone_number": "$phoneNumber",
                                "email": "$email"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, AddressBookEntryResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    AddressBookEntryResponse(
                        id = response.id,
                        alias = alias,
                        address = address.rawValue,
                        phoneNumber = phoneNumber,
                        email = email,
                        createdAt = response.createdAt
                    )
                )

            expectThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("address book entry is correctly stored into the database") {
            val storedEntry = addressBookRepository.getById(response.id)!!

            expectThat(storedEntry)
                .isEqualTo(
                    AddressBookEntry(
                        id = response.id,
                        alias = alias,
                        address = address,
                        phoneNumber = phoneNumber,
                        email = email,
                        createdAt = storedEntry.createdAt,
                        userId = OWNER_ID
                    )
                )

            expectThat(storedEntry.createdAt.value)
                .isCloseTo(response.createdAt, WITHIN_TIME_TOLERANCE)
            expectThat(storedEntry.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    @WithMockUser(OWNER_ADDRESS)
    fun mustCorrectlyUpdateAddressBookEntry() {
        val entry = AddressBookEntry(
            id = AddressBookId(UUID.randomUUID()),
            alias = "alias",
            address = WalletAddress("abc"),
            phoneNumber = "phone-number",
            email = "email",
            createdAt = TestData.TIMESTAMP,
            userId = OWNER_ID
        )

        suppose("some address book entry exists in the database") {
            addressBookRepository.store(entry)
        }

        val newAlias = "alias"
        val newAddress = WalletAddress("abc")
        val newPhoneNumber = "phone-number"
        val newEmail = "email"

        val response = suppose("request to update address book entry is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.patch("/v1/address-book/${entry.id.value}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "$newAlias",
                                "address": "${newAddress.rawValue}",
                                "phone_number": "$newPhoneNumber",
                                "email": "$newEmail"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, AddressBookEntryResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    AddressBookEntryResponse(
                        id = entry.id,
                        alias = newAlias,
                        address = newAddress.rawValue,
                        phoneNumber = newPhoneNumber,
                        email = newEmail,
                        createdAt = entry.createdAt.value
                    )
                )
        }

        verify("address book entry is correctly updated in the database") {
            val storedEntry = addressBookRepository.getById(response.id)!!

            expectThat(storedEntry)
                .isEqualTo(
                    AddressBookEntry(
                        id = entry.id,
                        alias = newAlias,
                        address = newAddress,
                        phoneNumber = newPhoneNumber,
                        email = newEmail,
                        createdAt = entry.createdAt,
                        userId = entry.userId
                    )
                )
        }
    }

    @Test
    @WithMockUser(OTHER_OWNER_ADDRESS)
    fun mustReturn404NotFoundWhenUpdatingNonOwnedAddressBookEntry() {
        val entry = AddressBookEntry(
            id = AddressBookId(UUID.randomUUID()),
            alias = "alias",
            address = WalletAddress("abc"),
            phoneNumber = "phone-number",
            email = "email",
            createdAt = TestData.TIMESTAMP,
            userId = OWNER_ID
        )

        suppose("some address book entry exists in the database") {
            addressBookRepository.store(entry)
        }

        val newAlias = "alias"
        val newAddress = WalletAddress("abc")
        val newPhoneNumber = "phone-number"
        val newEmail = "email"

        verify("404 is returned for non-owned address book entry") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.patch("/v1/address-book/${entry.id.value}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "$newAlias",
                                "address": "${newAddress.rawValue}",
                                "phone_number": "$newPhoneNumber",
                                "email": "$newEmail"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    @WithMockUser(OWNER_ADDRESS)
    fun mustReturn404NotFoundWhenUpdatingNonExistentAddressBookEntry() {
        val alias = "alias"
        val address = WalletAddress("abc")
        val phoneNumber = "phone-number"
        val email = "email"

        verify("404 is returned for non-existent address book entry") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.patch("/v1/address-book/${UUID.randomUUID()}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "alias": "$alias",
                                "address": "${address.rawValue}",
                                "phone_number": "$phoneNumber",
                                "email": "$email"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    @WithMockUser(OWNER_ADDRESS)
    fun mustCorrectlyDeleteAddressBookEntry() {
        val entry = AddressBookEntry(
            id = AddressBookId(UUID.randomUUID()),
            alias = "alias",
            address = WalletAddress("abc"),
            phoneNumber = "phone-number",
            email = "email",
            createdAt = TestData.TIMESTAMP,
            userId = OWNER_ID
        )

        suppose("some address book entry exists in the database") {
            addressBookRepository.store(entry)
        }

        suppose("request to delete address book entry is made") {
            mockMvc.perform(
                MockMvcRequestBuilders.delete("/v1/address-book/${entry.id.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
        }

        verify("address book entry is deleted from database") {
            expectThat(addressBookRepository.getById(entry.id))
                .isNull()
        }
    }

    @Test
    @WithMockUser(OTHER_OWNER_ADDRESS)
    fun mustReturn404NotFoundWhenDeletingNonOwnedAddressBookEntry() {
        val entry = AddressBookEntry(
            id = AddressBookId(UUID.randomUUID()),
            alias = "alias",
            address = WalletAddress("abc"),
            phoneNumber = "phone-number",
            email = "email",
            createdAt = TestData.TIMESTAMP,
            userId = OWNER_ID
        )

        suppose("some address book entry exists in the database") {
            addressBookRepository.store(entry)
        }

        verify("404 is returned for non-owned address book entry") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.delete("/v1/address-book/${entry.id.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    @WithMockUser(OWNER_ADDRESS)
    fun mustReturn404NotFoundWhenDeletingNonExistentAddressBookEntry() {
        verify("404 is returned for non-existent address book entry") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.delete("/v1/address-book/${UUID.randomUUID()}")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustCorrectlyFetchAddressBookEntryById() {
        val entry = AddressBookEntry(
            id = AddressBookId(UUID.randomUUID()),
            alias = "alias",
            address = WalletAddress("abc"),
            phoneNumber = "phone-number",
            email = "email",
            createdAt = TestData.TIMESTAMP,
            userId = OWNER_ID
        )

        suppose("some address book entry exists in the database") {
            addressBookRepository.store(entry)
        }

        val response = suppose("request to fetch address book entry by id is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/address-book/${entry.id.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, AddressBookEntryResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    AddressBookEntryResponse(
                        id = entry.id,
                        alias = entry.alias,
                        address = entry.address.rawValue,
                        phoneNumber = entry.phoneNumber,
                        email = entry.email,
                        createdAt = entry.createdAt.value
                    )
                )
        }
    }

    @Test
    fun mustReturn404NotFoundWhenFetchingNonExistentAddressBookEntryById() {
        verify("404 is returned for non-existent address book entry") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/address-book/${UUID.randomUUID()}")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    @WithMockUser(OWNER_ADDRESS)
    fun mustCorrectlyFetchAddressBookEntryByAlias() {
        val entry = AddressBookEntry(
            id = AddressBookId(UUID.randomUUID()),
            alias = "alias",
            address = WalletAddress("abc"),
            phoneNumber = "phone-number",
            email = "email",
            createdAt = TestData.TIMESTAMP,
            userId = OWNER_ID
        )

        suppose("some address book entry exists in the database") {
            addressBookRepository.store(entry)
        }

        val response = suppose("request to fetch address book entry by alias is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/address-book/by-alias/${entry.alias}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, AddressBookEntryResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    AddressBookEntryResponse(
                        id = entry.id,
                        alias = entry.alias,
                        address = entry.address.rawValue,
                        phoneNumber = entry.phoneNumber,
                        email = entry.email,
                        createdAt = entry.createdAt.value
                    )
                )
        }
    }

    @Test
    @WithMockUser(OWNER_ADDRESS)
    fun mustReturn404NotFoundWhenFetchingNonExistentAddressBookEntryByAlias() {
        verify("404 is returned for non-existent address book entry") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/address-book/by-alias/non-existent-alias")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustCorrectlyFetchAddressBookEntriesForWalletAddress() {
        val entry = AddressBookEntry(
            id = AddressBookId(UUID.randomUUID()),
            alias = "alias",
            address = WalletAddress("abc"),
            phoneNumber = "phone-number",
            email = "email",
            createdAt = TestData.TIMESTAMP,
            userId = OWNER_ID
        )

        suppose("some address book entry exists in the database") {
            addressBookRepository.store(entry)
        }

        val response = suppose("request to fetch address book entries for wallet address is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/v1/address-book/by-wallet-address/${WalletAddress(OWNER_ADDRESS).rawValue}"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, AddressBookEntriesResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    AddressBookEntriesResponse(
                        listOf(
                            AddressBookEntryResponse(
                                id = entry.id,
                                alias = entry.alias,
                                address = entry.address.rawValue,
                                phoneNumber = entry.phoneNumber,
                                email = entry.email,
                                createdAt = entry.createdAt.value
                            )
                        )
                    )
                )
        }
    }
}
