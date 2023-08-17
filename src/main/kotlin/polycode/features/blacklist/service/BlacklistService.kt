package polycode.features.blacklist.service

import polycode.features.api.access.model.result.UserIdentifier
import polycode.util.EthereumAddress
import polycode.util.WalletAddress

interface BlacklistService {
    fun addAddress(userIdentifier: UserIdentifier, address: EthereumAddress)
    fun removeAddress(userIdentifier: UserIdentifier, address: EthereumAddress)
    fun listAddresses(userIdentifier: UserIdentifier): List<WalletAddress>
}
