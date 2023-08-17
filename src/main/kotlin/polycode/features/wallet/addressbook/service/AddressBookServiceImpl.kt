package polycode.features.wallet.addressbook.service

import mu.KLogging
import org.springframework.stereotype.Service
import polycode.exception.ResourceNotFoundException
import polycode.features.api.access.model.result.UserIdentifier
import polycode.features.wallet.addressbook.model.request.CreateOrUpdateAddressBookEntryRequest
import polycode.features.wallet.addressbook.model.result.AddressBookEntry
import polycode.features.wallet.addressbook.repository.AddressBookRepository
import polycode.generated.jooq.id.AddressBookId
import polycode.service.UtcDateTimeProvider
import polycode.service.UuidProvider
import polycode.util.WalletAddress

@Service
class AddressBookServiceImpl(
    private val addressBookRepository: AddressBookRepository,
    private val uuidProvider: UuidProvider,
    private val utcDateTimeProvider: UtcDateTimeProvider
) : AddressBookService {

    companion object : KLogging()

    override fun createAddressBookEntry(
        request: CreateOrUpdateAddressBookEntryRequest,
        userIdentifier: UserIdentifier
    ): AddressBookEntry {
        logger.info { "Creating address book entry, request: $request, userIdentifier: $userIdentifier" }
        return addressBookRepository.store(
            AddressBookEntry(
                id = uuidProvider.getUuid(AddressBookId),
                createdAt = utcDateTimeProvider.getUtcDateTime(),
                request = request,
                userIdentifier = userIdentifier
            )
        )
    }

    override fun updateAddressBookEntry(
        addressBookEntryId: AddressBookId,
        request: CreateOrUpdateAddressBookEntryRequest,
        userIdentifier: UserIdentifier
    ): AddressBookEntry {
        logger.info {
            "Update address book entry, addressBookEntryId: $addressBookEntryId, request: $request," +
                " userIdentifier: $userIdentifier"
        }
        val entry = getOwnedAddressBookEntryById(addressBookEntryId, userIdentifier)
        return addressBookRepository.update(
            AddressBookEntry(
                id = entry.id,
                createdAt = entry.createdAt,
                request = request,
                userIdentifier = userIdentifier
            )
        ) ?: throw ResourceNotFoundException("Address book entry not found for ID: $addressBookEntryId")
    }

    override fun deleteAddressBookEntryById(id: AddressBookId, userIdentifier: UserIdentifier) {
        logger.info { "Delete address book entry by id: $id, userIdentifier: $userIdentifier" }
        addressBookRepository.delete(getOwnedAddressBookEntryById(id, userIdentifier).id)
    }

    override fun getAddressBookEntryById(id: AddressBookId): AddressBookEntry {
        logger.debug { "Get address book entry by id: $id" }
        return addressBookRepository.getById(id)
            ?: throw ResourceNotFoundException("Address book entry not found for ID: $id")
    }

    override fun getAddressBookEntryByAlias(alias: String, userIdentifier: UserIdentifier): AddressBookEntry {
        logger.debug { "Get address book entry by alias: $alias, userIdentifier: $userIdentifier" }
        return addressBookRepository.getByAliasAndUserId(alias, userIdentifier.id)
            ?: throw ResourceNotFoundException("Address book entry not found for alias: $alias")
    }

    override fun getAddressBookEntriesByWalletAddress(walletAddress: WalletAddress): List<AddressBookEntry> {
        logger.debug { "Get address book entries by walletAddress: $walletAddress" }
        return addressBookRepository.getAllByWalletAddress(walletAddress)
    }

    private fun getOwnedAddressBookEntryById(id: AddressBookId, userIdentifier: UserIdentifier) =
        getAddressBookEntryById(id).takeIf { it.userId == userIdentifier.id }
            ?: throw ResourceNotFoundException("Address book entry not found for ID: $id")
}
