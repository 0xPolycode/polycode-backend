package polycode.features.blacklist.repository

import mu.KLogging
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import polycode.generated.jooq.tables.BlacklistedAddressTable
import polycode.generated.jooq.tables.records.BlacklistedAddressRecord
import polycode.util.EthereumAddress
import polycode.util.WalletAddress

@Repository
class JooqBlacklistedAddressRepository(private val dslContext: DSLContext) : BlacklistedAddressRepository {

    companion object : KLogging()

    override fun addAddress(address: EthereumAddress) {
        logger.info { "Add address to blacklist: $address" }
        dslContext.insertInto(BlacklistedAddressTable)
            .set(BlacklistedAddressRecord(address.toWalletAddress()))
            .onConflictDoNothing()
            .execute()
    }

    override fun removeAddress(address: EthereumAddress) {
        logger.info { "Remove address from blacklist: $address" }
        dslContext.deleteFrom(BlacklistedAddressTable)
            .where(BlacklistedAddressTable.WALLET_ADDRESS.eq(address.toWalletAddress()))
            .execute()
    }

    override fun exists(address: EthereumAddress): Boolean {
        logger.debug { "Check if address is on blacklist: $address" }
        return dslContext.fetchExists(
            BlacklistedAddressTable,
            BlacklistedAddressTable.WALLET_ADDRESS.eq(address.toWalletAddress())
        )
    }

    override fun listAddresses(): List<WalletAddress> {
        logger.debug { "List blacklisted addresses" }
        return dslContext.selectFrom(BlacklistedAddressTable)
            .fetch { it.walletAddress }
    }
}
