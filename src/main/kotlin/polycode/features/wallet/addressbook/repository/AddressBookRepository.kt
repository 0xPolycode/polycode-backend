package polycode.features.wallet.addressbook.repository

import polycode.features.wallet.addressbook.model.result.AddressBookEntry
import polycode.generated.jooq.id.AddressBookId
import polycode.generated.jooq.id.UserId
import polycode.util.WalletAddress

interface AddressBookRepository {
    fun store(addressBookEntry: AddressBookEntry): AddressBookEntry
    fun update(addressBookEntry: AddressBookEntry): AddressBookEntry?
    fun delete(id: AddressBookId): Boolean
    fun getById(id: AddressBookId): AddressBookEntry?
    fun getByAliasAndUserId(alias: String, userId: UserId): AddressBookEntry?
    fun getAllByWalletAddress(walletAddress: WalletAddress): List<AddressBookEntry>
}
