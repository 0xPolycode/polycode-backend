package polycode.features.api.access.repository

import polycode.features.api.access.model.result.UserIdentifier
import polycode.features.api.access.model.result.UserWalletAddressIdentifier
import polycode.generated.jooq.enums.UserIdentifierType
import polycode.generated.jooq.id.UserId
import polycode.util.WalletAddress

interface UserIdentifierRepository {
    fun <T : UserIdentifier> store(userIdentifier: T): T
    fun getById(id: UserId): UserIdentifier?
    fun getByUserIdentifier(userIdentifier: String, identifierType: UserIdentifierType): UserIdentifier?

    fun getByWalletAddress(walletAddress: WalletAddress): UserWalletAddressIdentifier? =
        getByUserIdentifier(walletAddress.rawValue, UserIdentifierType.ETH_WALLET_ADDRESS)?.let {
            UserWalletAddressIdentifier(
                id = it.id,
                walletAddress = WalletAddress(it.userIdentifier)
            )
        }
}
