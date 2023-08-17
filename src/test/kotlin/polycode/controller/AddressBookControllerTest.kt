package polycode.controller

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import polycode.JsonSchemaDocumentation
import polycode.TestBase
import polycode.TestData
import polycode.features.api.access.model.result.UserWalletAddressIdentifier
import polycode.features.wallet.addressbook.controller.AddressBookController
import polycode.features.wallet.addressbook.model.request.CreateOrUpdateAddressBookEntryRequest
import polycode.features.wallet.addressbook.model.response.AddressBookEntriesResponse
import polycode.features.wallet.addressbook.model.response.AddressBookEntryResponse
import polycode.features.wallet.addressbook.model.result.AddressBookEntry
import polycode.features.wallet.addressbook.service.AddressBookService
import polycode.generated.jooq.id.AddressBookId
import polycode.generated.jooq.id.UserId
import polycode.util.WalletAddress
import java.util.UUID

class AddressBookControllerTest : TestBase() {

    companion object {
        private val USER_IDENTIFIER = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            walletAddress = WalletAddress("cafebabe")
        )
        private val ENTRY = AddressBookEntry(
            id = AddressBookId(UUID.randomUUID()),
            alias = "alias",
            address = WalletAddress("a"),
            phoneNumber = "phone-number",
            email = "email",
            createdAt = TestData.TIMESTAMP,
            userId = USER_IDENTIFIER.id
        )
    }

    @Test
    fun mustCorrectlyCreateAddressBookEntry() {
        val request = CreateOrUpdateAddressBookEntryRequest(
            alias = ENTRY.alias,
            address = ENTRY.address.rawValue,
            phoneNumber = ENTRY.phoneNumber,
            email = ENTRY.email
        )

        val service = mock<AddressBookService>()

        suppose("address book entry will be created") {
            call(service.createAddressBookEntry(request, USER_IDENTIFIER))
                .willReturn(ENTRY)
        }

        val controller = AddressBookController(service)

        verify("controller returns correct response") {
            val response = controller.createAddressBookEntry(USER_IDENTIFIER, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(AddressBookEntryResponse(ENTRY)))
        }
    }

    @Test
    fun mustCorrectlyUpdateAddressBookEntry() {
        val request = CreateOrUpdateAddressBookEntryRequest(
            alias = ENTRY.alias,
            address = ENTRY.address.rawValue,
            phoneNumber = ENTRY.phoneNumber,
            email = ENTRY.email
        )

        val service = mock<AddressBookService>()

        suppose("address book entry will be updated") {
            call(service.updateAddressBookEntry(ENTRY.id, request, USER_IDENTIFIER))
                .willReturn(ENTRY)
        }

        val controller = AddressBookController(service)

        verify("controller returns correct response") {
            val response = controller.updateAddressBookEntry(ENTRY.id, USER_IDENTIFIER, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(AddressBookEntryResponse(ENTRY)))
        }
    }

    @Test
    fun mustCorrectlyDeleteAddressBookEntry() {
        val service = mock<AddressBookService>()
        val controller = AddressBookController(service)

        verify("controller calls correct service method") {
            controller.deleteAddressBookEntry(ENTRY.id, USER_IDENTIFIER)

            expectInteractions(service) {
                once.deleteAddressBookEntryById(ENTRY.id, USER_IDENTIFIER)
            }
        }
    }

    @Test
    fun mustCorrectlyFetchAddressBookEntryById() {
        val service = mock<AddressBookService>()

        suppose("address book entry will be fetched by id") {
            call(service.getAddressBookEntryById(ENTRY.id))
                .willReturn(ENTRY)
        }

        val controller = AddressBookController(service)

        verify("controller returns correct response") {
            val response = controller.getAddressBookEntryById(ENTRY.id)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(AddressBookEntryResponse(ENTRY)))
        }
    }

    @Test
    fun mustCorrectlyFetchAddressBookEntryByAlias() {
        val service = mock<AddressBookService>()

        suppose("address book entry will be fetched by alias") {
            call(service.getAddressBookEntryByAlias(ENTRY.alias, USER_IDENTIFIER))
                .willReturn(ENTRY)
        }

        val controller = AddressBookController(service)

        verify("controller returns correct response") {
            val response = controller.getAddressBookEntryByAlias(ENTRY.alias, USER_IDENTIFIER)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(AddressBookEntryResponse(ENTRY)))
        }
    }

    @Test
    fun mustCorrectlyFetchAddressBookEntriesForWalletAddress() {
        val service = mock<AddressBookService>()

        suppose("address book entries will be fetched for wallet address") {
            call(service.getAddressBookEntriesByWalletAddress(USER_IDENTIFIER.walletAddress))
                .willReturn(listOf(ENTRY))
        }

        val controller = AddressBookController(service)

        verify("controller returns correct response") {
            val response = controller.getAddressBookEntriesForWalletAddress(USER_IDENTIFIER.walletAddress.rawValue)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        AddressBookEntriesResponse(
                            listOf(AddressBookEntryResponse(ENTRY))
                        )
                    )
                )
        }
    }
}
