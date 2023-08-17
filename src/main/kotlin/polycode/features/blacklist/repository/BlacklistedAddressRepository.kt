package polycode.features.blacklist.repository

import polycode.util.EthereumAddress
import polycode.util.WalletAddress

interface BlacklistedAddressRepository {
    fun addAddress(address: EthereumAddress)
    fun removeAddress(address: EthereumAddress)
    fun exists(address: EthereumAddress): Boolean
    fun listAddresses(): List<WalletAddress>
}
