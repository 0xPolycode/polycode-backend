package polycode.features.wallet.addressbook.model.result

import polycode.features.api.access.model.result.UserIdentifier
import polycode.features.wallet.addressbook.model.request.CreateOrUpdateAddressBookEntryRequest
import polycode.generated.jooq.id.AddressBookId
import polycode.generated.jooq.id.UserId
import polycode.util.UtcDateTime
import polycode.util.WalletAddress

data class AddressBookEntry(
    val id: AddressBookId,
    val alias: String,
    val address: WalletAddress,
    val phoneNumber: String?,
    val email: String?,
    val createdAt: UtcDateTime,
    val userId: UserId
) {
    constructor(
        id: AddressBookId,
        createdAt: UtcDateTime,
        request: CreateOrUpdateAddressBookEntryRequest,
        userIdentifier: UserIdentifier
    ) : this(
        id = id,
        alias = request.alias,
        address = WalletAddress(request.address),
        phoneNumber = request.phoneNumber,
        email = request.email,
        createdAt = createdAt,
        userId = userIdentifier.id
    )
}
