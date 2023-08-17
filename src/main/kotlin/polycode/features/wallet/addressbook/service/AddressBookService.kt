package polycode.features.wallet.addressbook.service

import polycode.features.api.access.model.result.UserIdentifier
import polycode.features.wallet.addressbook.model.request.CreateOrUpdateAddressBookEntryRequest
import polycode.features.wallet.addressbook.model.result.AddressBookEntry
import polycode.generated.jooq.id.AddressBookId
import polycode.util.WalletAddress

interface AddressBookService {
    fun createAddressBookEntry(
        request: CreateOrUpdateAddressBookEntryRequest,
        userIdentifier: UserIdentifier
    ): AddressBookEntry

    fun updateAddressBookEntry(
        addressBookEntryId: AddressBookId,
        request: CreateOrUpdateAddressBookEntryRequest,
        userIdentifier: UserIdentifier
    ): AddressBookEntry

    fun deleteAddressBookEntryById(id: AddressBookId, userIdentifier: UserIdentifier)
    fun getAddressBookEntryById(id: AddressBookId): AddressBookEntry
    fun getAddressBookEntryByAlias(alias: String, userIdentifier: UserIdentifier): AddressBookEntry
    fun getAddressBookEntriesByWalletAddress(walletAddress: WalletAddress): List<AddressBookEntry>
}
