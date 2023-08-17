package polycode.features.wallet.addressbook.controller

import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import polycode.config.binding.annotation.UserIdentifierBinding
import polycode.config.validation.ValidEthAddress
import polycode.features.api.access.model.result.UserIdentifier
import polycode.features.wallet.addressbook.model.request.CreateOrUpdateAddressBookEntryRequest
import polycode.features.wallet.addressbook.model.response.AddressBookEntriesResponse
import polycode.features.wallet.addressbook.model.response.AddressBookEntryResponse
import polycode.features.wallet.addressbook.service.AddressBookService
import polycode.generated.jooq.id.AddressBookId
import polycode.util.WalletAddress
import javax.validation.Valid

@Validated
@RestController
class AddressBookController(private val addressBookService: AddressBookService) {

    @PostMapping("/v1/address-book")
    fun createAddressBookEntry(
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @Valid @RequestBody requestBody: CreateOrUpdateAddressBookEntryRequest
    ): ResponseEntity<AddressBookEntryResponse> {
        return ResponseEntity.ok(
            AddressBookEntryResponse(
                addressBookService.createAddressBookEntry(
                    request = requestBody,
                    userIdentifier = userIdentifier
                )
            )
        )
    }

    @PatchMapping("/v1/address-book/{id}")
    fun updateAddressBookEntry(
        @PathVariable("id") id: AddressBookId,
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @Valid @RequestBody requestBody: CreateOrUpdateAddressBookEntryRequest
    ): ResponseEntity<AddressBookEntryResponse> {
        return ResponseEntity.ok(
            AddressBookEntryResponse(
                addressBookService.updateAddressBookEntry(
                    addressBookEntryId = id,
                    request = requestBody,
                    userIdentifier = userIdentifier
                )
            )
        )
    }

    @DeleteMapping("/v1/address-book/{id}")
    fun deleteAddressBookEntry(
        @PathVariable("id") id: AddressBookId,
        @UserIdentifierBinding userIdentifier: UserIdentifier
    ) {
        addressBookService.deleteAddressBookEntryById(id, userIdentifier)
    }

    @GetMapping("/v1/address-book/{id}")
    fun getAddressBookEntryById(
        @PathVariable("id") id: AddressBookId
    ): ResponseEntity<AddressBookEntryResponse> {
        return ResponseEntity.ok(AddressBookEntryResponse(addressBookService.getAddressBookEntryById(id)))
    }

    @GetMapping("/v1/address-book/by-alias/{alias}")
    fun getAddressBookEntryByAlias(
        @PathVariable("alias") alias: String,
        @UserIdentifierBinding userIdentifier: UserIdentifier
    ): ResponseEntity<AddressBookEntryResponse> {
        return ResponseEntity.ok(
            AddressBookEntryResponse(
                addressBookService.getAddressBookEntryByAlias(
                    alias = alias,
                    userIdentifier = userIdentifier
                )
            )
        )
    }

    @GetMapping("/v1/address-book/by-wallet-address/{walletAddress}")
    fun getAddressBookEntriesForWalletAddress(
        @ValidEthAddress @PathVariable walletAddress: String
    ): ResponseEntity<AddressBookEntriesResponse> {
        return ResponseEntity.ok(
            AddressBookEntriesResponse(
                addressBookService.getAddressBookEntriesByWalletAddress(WalletAddress(walletAddress)).map {
                    AddressBookEntryResponse(it)
                }
            )
        )
    }
}
