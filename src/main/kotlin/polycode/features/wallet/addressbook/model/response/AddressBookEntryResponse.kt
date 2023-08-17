package polycode.features.wallet.addressbook.model.response

import polycode.features.wallet.addressbook.model.result.AddressBookEntry
import polycode.generated.jooq.id.AddressBookId
import java.time.OffsetDateTime

data class AddressBookEntryResponse(
    val id: AddressBookId,
    val alias: String,
    val address: String,
    val phoneNumber: String?,
    val email: String?,
    val createdAt: OffsetDateTime
) {
    constructor(entry: AddressBookEntry) : this(
        id = entry.id,
        alias = entry.alias,
        address = entry.address.rawValue,
        phoneNumber = entry.phoneNumber,
        email = entry.email,
        createdAt = entry.createdAt.value
    )
}
