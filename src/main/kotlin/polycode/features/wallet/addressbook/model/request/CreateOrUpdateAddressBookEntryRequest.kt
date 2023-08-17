package polycode.features.wallet.addressbook.model.request

import polycode.config.validation.MaxStringSize
import polycode.config.validation.ValidAlias
import polycode.config.validation.ValidEthAddress
import javax.validation.constraints.NotNull

data class CreateOrUpdateAddressBookEntryRequest(
    @field:NotNull
    @field:ValidAlias
    val alias: String,
    @field:NotNull
    @field:ValidEthAddress
    val address: String,
    @field:MaxStringSize
    val phoneNumber: String?,
    @field:MaxStringSize
    val email: String?
)
